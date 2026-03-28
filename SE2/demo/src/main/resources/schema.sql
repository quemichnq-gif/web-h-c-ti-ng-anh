CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(100) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS authorities (
    username VARCHAR(100) NOT NULL,
    authority VARCHAR(100) NOT NULL,
    PRIMARY KEY (username, authority),
    CONSTRAINT fk_authorities_users
        FOREIGN KEY (username) REFERENCES users (username)
        ON DELETE CASCADE
);
