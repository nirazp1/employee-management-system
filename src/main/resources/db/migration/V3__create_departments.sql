CREATE TABLE departments (
    id              UUID PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(1000),
    manager_id      UUID,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    CONSTRAINT uq_departments_name UNIQUE (name)
);
