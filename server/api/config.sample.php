<?php
/**
 * Copy this file to config.php and fill in your alwaysdata database details.
 * config.php must NEVER be committed or shared.
 *
 * alwaysdata: Databases > MySQL. Host looks like  mysql-<account>.alwaysdata.net
 * Database and user names are prefixed with your account name, e.g. muziki_app.
 */

define('DB_HOST', 'mysql-muziki.alwaysdata.net');
define('DB_NAME', 'muziki_app');
define('DB_USER', 'muziki_app');
define('DB_PASS', 'CHANGE_ME');

// Public URL where uploaded avatars are served from (no trailing slash).
define('AVATAR_BASE_URL', 'https://muziki.alwaysdata.net/api/uploads/avatars');

// How long a login stays valid without being renewed by use.
define('TOKEN_TTL_DAYS', 90);

// Max simultaneous logged-in devices per account (oldest are signed out).
define('MAX_TOKENS_PER_USER', 10);

// Set true only if the app sits behind a trusted reverse proxy that sets X-Forwarded-For.
define('TRUST_PROXY_HEADERS', false);
