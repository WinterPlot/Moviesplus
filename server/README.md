# AnyFilm auth server (UserLab Ltd)

PHP 8.1+ and MySQL. One endpoint: `POST /api/auth.php`.

## Deploy on alwaysdata (about 5 minutes)

1. **Database**: Databases > MySQL > *Add a database* (e.g. `muziki_app`) and a user with a strong password.
   Open phpMyAdmin, select the database, and **import `api/schema.sql`**.
2. **Upload** the whole `api/` folder to the site served at `https://muziki.alwaysdata.net/` (so the file is at `/api/auth.php`), e.g. with SCP/SFTP into `~/www/api/`.
3. **Config**: on the server copy `api/config.sample.php` to `api/config.php` and fill in `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS`.
4. **Permissions**: make sure `api/uploads/avatars/` is writable by PHP (`chmod 755`, or 775 if needed).
5. **PHP version**: Sites > your site > PHP version 8.1 or newer.
6. **Test**:
   ```
   curl -s -X POST https://muziki.alwaysdata.net/api/auth.php \
        -H 'Content-Type: application/json' \
        -d '{"action":"register","name":"Test User","email":"test@example.com","password":"Passw0rd!x"}'
   ```
   You should get `{"status":"success", ... "token": "...", "user": {...}}`.

## Actions

| action | needs token | fields | result |
|---|---|---|---|
| `register` | no | `name`, `email`, `password`, `device?` | `token`, `user` |
| `login` | no | `email`, `password`, `device?` | `token`, `user` |
| `me` | yes | n/a | `user` |
| `logout` | yes | n/a | revokes this device |
| `logout_all` | yes | n/a | revokes all devices |
| `update_profile` | yes | `name` | `user` |
| `upload_avatar` | yes | multipart field `avatar` (JPG/PNG/WebP, max 3 MB) | `user` |
| `change_password` | yes | `current_password`, `new_password` | signs out other devices |
| `delete_account` | yes | `password` | deletes account and avatar |

Token goes in `Authorization: Bearer <token>`.
Errors use HTTP status codes (400, 401, 403, 409, 413, 415, 422, 429, 500) and `{"status":"error","message":"..."}`.

## Security notes

- Passwords are hashed with `password_hash` (bcrypt) and are never stored or logged in plain text.
- Session tokens are random 256-bit values; only their SHA-256 hash is stored, so a database leak does not expose live sessions.
- All queries use prepared statements.
- Login is rate-limited (5 failures per email and 30 per IP in 15 minutes). Sign-up is limited to 10 per IP per hour.
- Unknown email and wrong password return the same message, and take comparable time.
- Avatars are re-encoded to a 512px JPEG, so anything hidden in an uploaded file is dropped. `uploads/` refuses to execute scripts.
- `config.php` holds your DB password. Never commit or share it.

## Not included yet

- Email verification and "forgot password" (needs SMTP set up on alwaysdata).
- Admin dashboard. Block a user by setting `users.status = 0` in phpMyAdmin.
