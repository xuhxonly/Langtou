CREATE TABLE IF NOT EXISTS game_session (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    game_id         BIGINT          NOT NULL,
    room_id         VARCHAR(64)     NOT NULL,
    host_user_id    BIGINT          NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'WAITING',
    started_at      TIMESTAMP        NULL,
    ended_at        TIMESTAMP        NULL,
    max_players     INT             NOT NULL DEFAULT 8,
    current_players INT             NOT NULL DEFAULT 1,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS game_session_players (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    session_id  BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    joined_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_session_user UNIQUE (session_id, user_id)
);

CREATE TABLE IF NOT EXISTS game_payment (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    order_no            VARCHAR(64)     NOT NULL,
    product_id          VARCHAR(128)    NOT NULL,
    amount              BIGINT          NOT NULL,
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY',
    channel             VARCHAR(32)     NOT NULL,
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    paid_at             TIMESTAMP        NULL,
    created_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_no UNIQUE (order_no)
);
