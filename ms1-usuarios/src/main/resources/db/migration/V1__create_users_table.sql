CREATE TABLE users (
                       id            BIGSERIAL    PRIMARY KEY,
                       name          VARCHAR(120) NOT NULL,
                       email         VARCHAR(160) NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       created_at    TIMESTAMP    NOT NULL,
                       CONSTRAINT uk_users_email UNIQUE (email)
);