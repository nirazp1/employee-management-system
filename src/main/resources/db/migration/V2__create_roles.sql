CREATE TABLE roles (
    id      UUID PRIMARY KEY,
    name    VARCHAR(50) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    user_id     UUID NOT NULL,
    role_id     UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'ADMIN'),
    (gen_random_uuid(), 'HR'),
    (gen_random_uuid(), 'MANAGER'),
    (gen_random_uuid(), 'EMPLOYEE');
