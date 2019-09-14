CREATE TABLE IF NOT EXISTS user
    (
        id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        role      TINYINT UNSIGNED NOT NULL,
        name      VARCHAR(255),
        PRIMARY KEY (id),
    );

CREATE TABLE IF NOT EXISTS post
    (
        id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        content     VARCHAR(2048),
        author_id   BIGINT UNSIGNED NOT NULL,
        PRIMARY KEY (id),
        FOREIGN KEY (author_id) REFERENCES user(id)
    );

CREATE TABLE IF NOT EXISTS comment
    (
        id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        author_id    BIGINT UNSIGNED NOT NULL,
        post_id      BIGINT UNSIGNED NOT NULL,
        content      VARCHAR(255),
        PRIMARY KEY (id),
        FOREIGN KEY (author_id) REFERENCES user(id),
        FOREIGN KEY (post_id) REFERENCES post(id)
    );
