CREATE SEQUENCE portal_ids START WITH 1 INCREMENT BY 1;

CREATE TABLE workflow_guard
(
    id INTEGER PRIMARY KEY
);

INSERT INTO workflow_guard(id)
VALUES (1);

CREATE TABLE shifts
(
    id         BIGINT PRIMARY KEY,
    shift_date DATE    NOT NULL,
    starts_at  TIME(6) NOT NULL,
    ends_at    TIME(6) NOT NULL,
    slot       INTEGER NOT NULL,
    building   INTEGER NOT NULL,
    room       VARCHAR(255),
    owner      VARCHAR(255),
    completed  BOOLEAN NOT NULL
);

CREATE TABLE availability
(
    id          BIGINT PRIMARY KEY,
    user_login  VARCHAR(255) NOT NULL,
    parity      INTEGER      NOT NULL,
    weekday     INTEGER      NOT NULL,
    slot        INTEGER      NOT NULL,
    description TEXT
);

CREATE TABLE exchange_offers
(
    id       BIGINT PRIMARY KEY,
    shift_id BIGINT       NOT NULL,
    owner    VARCHAR(255),
    status   VARCHAR(255) NOT NULL
);

CREATE TABLE exchange_requests
(
    id              BIGINT PRIMARY KEY,
    request_type    VARCHAR(255)                NOT NULL,
    initiator       VARCHAR(255)                NOT NULL,
    recipient       VARCHAR(255),
    offer_id        BIGINT,
    status          VARCHAR(255)                NOT NULL,
    request_comment TEXT,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE time_entries
(
    id         BIGINT PRIMARY KEY,
    user_login VARCHAR(255)                NOT NULL,
    shift_id   BIGINT                      NOT NULL,
    started_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ended_at   TIMESTAMP(6) WITH TIME ZONE
);

CREATE TABLE notifications
(
    id         BIGINT PRIMARY KEY,
    user_login VARCHAR(255)                NOT NULL,
    text       TEXT,
    is_read    BOOLEAN                     NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_events
(
    id         BIGINT PRIMARY KEY,
    actor      VARCHAR(255),
    action     VARCHAR(255),
    target     VARCHAR(255),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE users
(
    login          VARCHAR(255) PRIMARY KEY,
    name           VARCHAR(255)     NOT NULL,
    initials       VARCHAR(255),
    position       VARCHAR(255),
    department     VARCHAR(255),
    email          VARCHAR(255),
    employed_since VARCHAR(255),
    shifts         INTEGER          NOT NULL,
    hours          INTEGER          NOT NULL,
    resolved       INTEGER          NOT NULL,
    punctuality    INTEGER          NOT NULL,
    score          DOUBLE PRECISION NOT NULL,
    rating_rank    INTEGER          NOT NULL,
    role           VARCHAR(255)     NOT NULL,
    salt           BYTEA            NOT NULL,
    password_hash  BYTEA            NOT NULL,
    active         BOOLEAN          NOT NULL
);

CREATE TABLE user_sessions
(
    id         VARCHAR(255) PRIMARY KEY,
    login      VARCHAR(255)                NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE exchange_offers_declined_by
(
    entity_id  BIGINT       NOT NULL REFERENCES exchange_offers (id),
    item_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (entity_id, item_value)
);

CREATE TABLE exchange_requests_shift_ids
(
    entity_id      BIGINT  NOT NULL REFERENCES exchange_requests (id),
    item_value     BIGINT  NOT NULL,
    position_index INTEGER NOT NULL,
    PRIMARY KEY (entity_id, position_index)
);

CREATE TABLE users_achievements
(
    entity_id      VARCHAR(255) NOT NULL REFERENCES users (login),
    item_value     VARCHAR(255) NOT NULL,
    position_index INTEGER      NOT NULL,
    PRIMARY KEY (entity_id, position_index)
);
