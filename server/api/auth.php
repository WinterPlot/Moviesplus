<?php
declare(strict_types=1);

/**
 * UserLab Ltd - AnyFilm authentication API
 *
 * Single endpoint:  POST https://muziki.alwaysdata.net/api/auth.php
 * Body: JSON  {"action": "...", ...}   (avatar upload uses multipart/form-data)
 * Auth: Authorization: Bearer <token>   (or X-Auth-Token: <token>)
 *
 * Actions
 *   register         name, email, password, device?          -> token + user
 *   login            email, password, device?                -> token + user
 *   me               (auth)                                  -> user
 *   logout           (auth)                                  -> revokes this device's token
 *   logout_all       (auth)                                  -> revokes every token
 *   update_profile   (auth) name                             -> user
 *   upload_avatar    (auth) multipart field "avatar"         -> user
 *   change_password  (auth) current_password, new_password   -> signs out other devices
 *   delete_account   (auth) password                         -> deletes account and data
 *
 * Every response: {"status":"success"|"error","message":"...", ...}
 */

require __DIR__ . '/config.php';

// ───────────────────────── response helpers ─────────────────────────

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

function respond(int $http, string $message, array $extra = []): never
{
    http_response_code($http);
    echo json_encode(
        array_merge(['status' => $http < 400 ? 'success' : 'error', 'message' => $message], $extra),
        JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE
    );
    exit;
}

set_exception_handler(function (Throwable $e): void {
    error_log('[auth.php] ' . $e);
    respond(500, 'Something went wrong on our side. Please try again later.');
});

// ───────────────────────── database ─────────────────────────

function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        // DB_DSN lets automated tests point at SQLite; production uses the MySQL constants.
        $dsn = defined('DB_DSN')
            ? DB_DSN
            : sprintf('mysql:host=%s;dbname=%s;charset=utf8mb4', DB_HOST, DB_NAME);
        $pdo = new PDO($dsn, defined('DB_USER') ? DB_USER : null, defined('DB_PASS') ? DB_PASS : null, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ]);
    }
    return $pdo;
}

/** UTC timestamp in DB format. PHP generates all times so the SQL stays portable. */
function now(int $offsetSeconds = 0): string
{
    return gmdate('Y-m-d H:i:s', time() + $offsetSeconds);
}

// ───────────────────────── request helpers ─────────────────────────

function client_ip(): string
{
    if (TRUST_PROXY_HEADERS && !empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        $first = trim(explode(',', $_SERVER['HTTP_X_FORWARDED_FOR'])[0]);
        if (filter_var($first, FILTER_VALIDATE_IP)) {
            return $first;
        }
    }
    return substr((string)($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0'), 0, 45);
}

function read_input(): array
{
    $type = strtolower((string)($_SERVER['CONTENT_TYPE'] ?? ''));
    if (str_contains($type, 'application/json')) {
        $raw = (string)file_get_contents('php://input', false, null, 0, 65536);
        $data = json_decode($raw, true);
        if (!is_array($data)) {
            respond(400, 'Invalid request body.');
        }
        return $data;
    }
    return $_POST;
}

function str_field(array $in, string $key): string
{
    $v = $in[$key] ?? '';
    return is_string($v) ? $v : '';
}

function bearer_token(): string
{
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    if ($h === '' && function_exists('getallheaders')) {
        foreach (getallheaders() as $k => $v) {
            if (strcasecmp($k, 'Authorization') === 0) {
                $h = $v;
            }
        }
    }
    if (preg_match('/^Bearer\s+([A-Za-z0-9]{64})$/', trim($h), $m)) {
        return $m[1];
    }
    $x = trim((string)($_SERVER['HTTP_X_AUTH_TOKEN'] ?? ''));
    return preg_match('/^[A-Za-z0-9]{64}$/', $x) ? $x : '';
}

// ───────────────────────── validation ─────────────────────────

function clean_email(string $raw): string
{
    $email = strtolower(trim($raw));
    if (strlen($email) > 190 || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        respond(422, 'Please enter a valid email address.');
    }
    return $email;
}

function clean_name(string $raw): string
{
    $name = preg_replace('/[\x00-\x1F\x7F]/u', '', $raw) ?? '';
    $name = trim(preg_replace('/\s+/u', ' ', $name) ?? '');
    $len = mb_strlen($name);
    if ($len < 2 || $len > 40) {
        respond(422, 'Display name must be between 2 and 40 characters.');
    }
    return $name;
}

function check_new_password(string $pw): void
{
    // bcrypt only uses the first 72 bytes, so cap there instead of silently truncating.
    if (strlen($pw) < 8 || strlen($pw) > 72) {
        respond(422, 'Password must be between 8 and 72 characters.');
    }
}

function clean_device(string $raw): string
{
    $d = preg_replace('/[\x00-\x1F\x7F]/u', '', $raw) ?? '';
    return mb_substr(trim($d), 0, 120);
}

// ───────────────────────── rate limiting ─────────────────────────

function throttled(string $kind, string $emailHash, int $ipLimit, int $emailLimit, int $window): bool
{
    $since = now(-$window);
    $q = db()->prepare('SELECT COUNT(*) FROM auth_attempts WHERE kind = ? AND ip = ? AND created_at >= ?');
    $q->execute([$kind, client_ip(), $since]);
    if ((int)$q->fetchColumn() >= $ipLimit) {
        return true;
    }
    if ($emailLimit > 0) {
        $q = db()->prepare('SELECT COUNT(*) FROM auth_attempts WHERE kind = ? AND email_hash = ? AND created_at >= ?');
        $q->execute([$kind, $emailHash, $since]);
        if ((int)$q->fetchColumn() >= $emailLimit) {
            return true;
        }
    }
    return false;
}

function record_attempt(string $kind, string $emailHash): void
{
    db()->prepare('INSERT INTO auth_attempts (kind, ip, email_hash, created_at) VALUES (?, ?, ?, ?)')
        ->execute([$kind, client_ip(), $emailHash, now()]);
    if (random_int(1, 50) === 1) { // opportunistic cleanup
        db()->prepare('DELETE FROM auth_attempts WHERE created_at < ?')->execute([now(-86400)]);
    }
}

function clear_attempts(string $kind, string $emailHash): void
{
    db()->prepare('DELETE FROM auth_attempts WHERE kind = ? AND email_hash = ?')->execute([$kind, $emailHash]);
}

// ───────────────────────── users & tokens ─────────────────────────

function user_payload(array $u): array
{
    $avatar = '';
    if (!empty($u['avatar_path'])) {
        $avatar = rtrim(AVATAR_BASE_URL, '/') . '/' . $u['avatar_path'];
    }
    return [
        'id'           => (string)$u['id'],
        'email'        => $u['email'],
        'display_name' => $u['display_name'],
        'avatar_url'   => $avatar,
        'created_at'   => $u['created_at'],
    ];
}

function find_user_by_id(int $id): ?array
{
    $q = db()->prepare('SELECT * FROM users WHERE id = ?');
    $q->execute([$id]);
    return $q->fetch() ?: null;
}

function issue_token(int $userId, string $device): string
{
    $token = bin2hex(random_bytes(32)); // 64 hex chars; only its SHA-256 is stored
    db()->prepare(
        'INSERT INTO auth_tokens (user_id, token_hash, device_name, ip, created_at, last_used_at, expires_at)
         VALUES (?, ?, ?, ?, ?, ?, ?)'
    )->execute([
        $userId, hash('sha256', $token), $device, client_ip(),
        now(), now(), now(TOKEN_TTL_DAYS * 86400),
    ]);

    // Keep only the newest MAX_TOKENS_PER_USER sessions.
    $q = db()->prepare('SELECT id FROM auth_tokens WHERE user_id = ? ORDER BY id DESC');
    $q->execute([$userId]);
    $ids = $q->fetchAll(PDO::FETCH_COLUMN);
    foreach (array_slice($ids, MAX_TOKENS_PER_USER) as $oldId) {
        db()->prepare('DELETE FROM auth_tokens WHERE id = ?')->execute([(int)$oldId]);
    }
    return $token;
}

/** @return array{user: array, token_id: int} */
function require_auth(): array
{
    $token = bearer_token();
    if ($token === '') {
        respond(401, 'Please sign in to continue.');
    }
    $q = db()->prepare(
        'SELECT t.id AS token_id, t.last_used_at, u.*
           FROM auth_tokens t JOIN users u ON u.id = t.user_id
          WHERE t.token_hash = ? AND t.expires_at > ? AND u.status = 1'
    );
    $q->execute([hash('sha256', $token), now()]);
    $row = $q->fetch();
    if (!$row) {
        respond(401, 'Your session has expired. Please sign in again.');
    }
    // Refresh "last used" and slide the expiry, at most every 10 minutes.
    if (strtotime($row['last_used_at'] . ' UTC') < time() - 600) {
        db()->prepare('UPDATE auth_tokens SET last_used_at = ?, expires_at = ? WHERE id = ?')
            ->execute([now(), now(TOKEN_TTL_DAYS * 86400), $row['token_id']]);
    }
    $tokenId = (int)$row['token_id'];
    unset($row['token_id'], $row['last_used_at']);
    return ['user' => $row, 'token_id' => $tokenId];
}

// A valid bcrypt hash of a random string. Verified against when the email is unknown,
// so "no such account" and "wrong password" take the same time.
const DUMMY_HASH = '$2y$10$LeXGbAPLCkP3vP5CMeC82OvM7pQ8NGQKT4NwQHIMgT0y33iWy5jru';

// ───────────────────────── actions ─────────────────────────

function action_register(array $in): never
{
    $name  = clean_name(str_field($in, 'name'));
    $email = clean_email(str_field($in, 'email'));
    $pw    = str_field($in, 'password');
    check_new_password($pw);
    $device = clean_device(str_field($in, 'device'));
    $emailHash = hash('sha256', $email);

    if (throttled('register', $emailHash, 10, 0, 3600)) {
        respond(429, 'Too many sign-up attempts. Please try again later.');
    }
    record_attempt('register', $emailHash);

    $q = db()->prepare('SELECT id FROM users WHERE email = ?');
    $q->execute([$email]);
    if ($q->fetch()) {
        respond(409, 'An account with this email already exists. Try signing in.');
    }

    try {
        db()->prepare(
            'INSERT INTO users (email, password_hash, display_name, status, created_at, updated_at, last_login_at)
             VALUES (?, ?, ?, 1, ?, ?, ?)'
        )->execute([$email, password_hash($pw, PASSWORD_DEFAULT), $name, now(), now(), now()]);
    } catch (PDOException $e) {
        if ($e->getCode() === '23000') { // unique violation: lost a race with another sign-up
            respond(409, 'An account with this email already exists. Try signing in.');
        }
        throw $e;
    }

    $userId = (int)db()->lastInsertId();
    $user = find_user_by_id($userId);
    respond(201, 'Account created successfully.', [
        'token' => issue_token($userId, $device),
        'user'  => user_payload($user),
    ]);
}

function action_login(array $in): never
{
    $email = clean_email(str_field($in, 'email'));
    $pw    = str_field($in, 'password');
    $device = clean_device(str_field($in, 'device'));
    if ($pw === '' || strlen($pw) > 1024) {
        respond(422, 'Please enter your password.');
    }
    $emailHash = hash('sha256', $email);

    if (throttled('login', $emailHash, 30, 5, 900)) {
        respond(429, 'Too many failed attempts. Please wait 15 minutes and try again.');
    }

    $q = db()->prepare('SELECT * FROM users WHERE email = ?');
    $q->execute([$email]);
    $user = $q->fetch() ?: null;

    $ok = password_verify($pw, $user['password_hash'] ?? DUMMY_HASH) && $user !== null;
    if (!$ok) {
        record_attempt('login', $emailHash);
        respond(401, 'Invalid email or password.');
    }
    if ((int)$user['status'] !== 1) {
        respond(403, 'This account has been disabled. Contact support.');
    }

    if (password_needs_rehash($user['password_hash'], PASSWORD_DEFAULT)) {
        db()->prepare('UPDATE users SET password_hash = ? WHERE id = ?')
            ->execute([password_hash($pw, PASSWORD_DEFAULT), $user['id']]);
    }
    db()->prepare('UPDATE users SET last_login_at = ? WHERE id = ?')->execute([now(), $user['id']]);
    clear_attempts('login', $emailHash);

    respond(200, 'Signed in successfully.', [
        'token' => issue_token((int)$user['id'], $device),
        'user'  => user_payload($user),
    ]);
}

function action_me(): never
{
    $auth = require_auth();
    respond(200, 'OK', ['user' => user_payload($auth['user'])]);
}

function action_logout(): never
{
    $auth = require_auth();
    db()->prepare('DELETE FROM auth_tokens WHERE id = ?')->execute([$auth['token_id']]);
    respond(200, 'Signed out.');
}

function action_logout_all(): never
{
    $auth = require_auth();
    db()->prepare('DELETE FROM auth_tokens WHERE user_id = ?')->execute([$auth['user']['id']]);
    respond(200, 'Signed out of all devices.');
}

function action_update_profile(array $in): never
{
    $auth = require_auth();
    $name = clean_name(str_field($in, 'name'));
    db()->prepare('UPDATE users SET display_name = ?, updated_at = ? WHERE id = ?')
        ->execute([$name, now(), $auth['user']['id']]);
    respond(200, 'Profile updated.', ['user' => user_payload(find_user_by_id((int)$auth['user']['id']))]);
}

function action_upload_avatar(): never
{
    $auth = require_auth();
    $file = $_FILES['avatar'] ?? null;
    if (!$file || ($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file($file['tmp_name'])) {
        respond(400, 'No image received.');
    }
    if ($file['size'] > 3 * 1024 * 1024) {
        respond(413, 'Image is too large (max 3 MB).');
    }
    if (!extension_loaded('gd')) {
        respond(501, 'Image processing is not available on this server.');
    }

    $mime = (new finfo(FILEINFO_MIME_TYPE))->file($file['tmp_name']);
    $src = match ($mime) {
        'image/jpeg' => @imagecreatefromjpeg($file['tmp_name']),
        'image/png'  => @imagecreatefrompng($file['tmp_name']),
        'image/webp' => @imagecreatefromwebp($file['tmp_name']),
        default      => false,
    };
    if (!$src) {
        respond(415, 'Please upload a JPG, PNG or WebP image.');
    }

    // Centre-crop to a square and cap at 512px. Re-encoding also strips metadata and
    // anything hidden inside the original file.
    $w = imagesx($src);
    $h = imagesy($src);
    $side = min($w, $h);
    $out = min($side, 512);
    $dst = imagecreatetruecolor($out, $out);
    imagefill($dst, 0, 0, imagecolorallocate($dst, 255, 255, 255));
    imagecopyresampled($dst, $src, 0, 0, (int)(($w - $side) / 2), (int)(($h - $side) / 2), $out, $out, $side, $side);

    $dir = __DIR__ . '/uploads/avatars';
    if (!is_dir($dir) && !mkdir($dir, 0755, true) && !is_dir($dir)) {
        respond(500, 'Could not store the image.');
    }
    $name = 'u' . $auth['user']['id'] . '_' . bin2hex(random_bytes(6)) . '.jpg';
    if (!imagejpeg($dst, $dir . '/' . $name, 85)) {
        respond(500, 'Could not store the image.');
    }

    $old = $auth['user']['avatar_path'] ?? null;
    db()->prepare('UPDATE users SET avatar_path = ?, updated_at = ? WHERE id = ?')
        ->execute([$name, now(), $auth['user']['id']]);
    if ($old && preg_match('/^u\d+_[a-f0-9]{12}\.jpg$/', $old)) {
        @unlink($dir . '/' . $old);
    }
    respond(200, 'Profile photo updated.', ['user' => user_payload(find_user_by_id((int)$auth['user']['id']))]);
}

function action_change_password(array $in): never
{
    $auth = require_auth();
    $current = str_field($in, 'current_password');
    $new = str_field($in, 'new_password');
    check_new_password($new);
    if (!password_verify($current, $auth['user']['password_hash'])) {
        respond(403, 'Your current password is incorrect.');
    }
    db()->prepare('UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?')
        ->execute([password_hash($new, PASSWORD_DEFAULT), now(), $auth['user']['id']]);
    db()->prepare('DELETE FROM auth_tokens WHERE user_id = ? AND id <> ?')
        ->execute([$auth['user']['id'], $auth['token_id']]);
    respond(200, 'Password changed. Other devices were signed out.');
}

function action_delete_account(array $in): never
{
    $auth = require_auth();
    if (!password_verify(str_field($in, 'password'), $auth['user']['password_hash'])) {
        respond(403, 'Your password is incorrect.');
    }
    $avatar = $auth['user']['avatar_path'] ?? null;
    db()->prepare('DELETE FROM auth_tokens WHERE user_id = ?')->execute([$auth['user']['id']]);
    db()->prepare('DELETE FROM users WHERE id = ?')->execute([$auth['user']['id']]);
    if ($avatar && preg_match('/^u\d+_[a-f0-9]{12}\.jpg$/', $avatar)) {
        @unlink(__DIR__ . '/uploads/avatars/' . $avatar);
    }
    respond(200, 'Your account has been deleted.');
}

// ───────────────────────── router ─────────────────────────

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    header('Allow: POST');
    respond(405, 'Method not allowed.');
}

$input = read_input();
$action = str_field($input, 'action');

switch ($action) {
    case 'register':        action_register($input);
    case 'login':           action_login($input);
    case 'me':              action_me();
    case 'logout':          action_logout();
    case 'logout_all':      action_logout_all();
    case 'update_profile':  action_update_profile($input);
    case 'upload_avatar':   action_upload_avatar();
    case 'change_password': action_change_password($input);
    case 'delete_account':  action_delete_account($input);
    default:                respond(400, 'Unknown action.');
}
