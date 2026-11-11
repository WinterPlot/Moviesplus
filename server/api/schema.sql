-- Import once with phpMyAdmin (alwaysdata > Databases > MySQL > phpMyAdmin) into your database.

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(190)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    display_name    VARCHAR(60)     NOT NULL,
    avatar_path     VARCHAR(255)    NULL,
    status          TINYINT         NOT NULL DEFAULT 1,      -- 1 = active, 0 = blocked
    created_at      DATETIME        NOT NULL,
    updated_at      DATETIME        NOT NULL,
    last_login_at   DATETIME        NULL,
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_tokens (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED NOT NULL,
    token_hash      CHAR(64)        NOT NULL,                 -- sha256 of the token; the token itself is never stored
    device_name     VARCHAR(120)    NOT NULL DEFAULT '',
    ip              VARCHAR(45)     NOT NULL DEFAULT '',
    created_at      DATETIME        NOT NULL,
    last_used_at    DATETIME        NOT NULL,
    expires_at      DATETIME        NOT NULL,
    UNIQUE KEY uq_token_hash (token_hash),
    KEY idx_tokens_user (user_id),
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_attempts (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    kind            VARCHAR(16)     NOT NULL,                 -- 'login' | 'register'
    ip              VARCHAR(45)     NOT NULL,
    email_hash      CHAR(64)        NOT NULL,
    created_at      DATETIME        NOT NULL,
    KEY idx_attempts_ip (kind, ip, created_at),
    KEY idx_attempts_email (kind, email_hash, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
