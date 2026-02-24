CREATE TABLE person (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL
);

CREATE TABLE address (
    id          BIGSERIAL PRIMARY KEY,
    person_id   BIGINT NOT NULL REFERENCES person(id),
    street      VARCHAR(200) NOT NULL,
    zip_code    VARCHAR(10)  NOT NULL,
    city        VARCHAR(100) NOT NULL
);

CREATE TABLE document (
    id          BIGSERIAL PRIMARY KEY,
    person_id   BIGINT NOT NULL REFERENCES person(id),
    filename    VARCHAR(255) NOT NULL,
    pdf_data    BYTEA        NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
