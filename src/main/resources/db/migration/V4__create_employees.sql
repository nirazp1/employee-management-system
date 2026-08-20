CREATE TABLE employees (
    id                  UUID PRIMARY KEY,
    employee_number     VARCHAR(50) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(30),
    date_of_birth       DATE,
    hire_date           DATE NOT NULL,
    job_title           VARCHAR(150) NOT NULL,
    salary              NUMERIC(14, 2) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    department_id       UUID,
    user_id             UUID,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    CONSTRAINT uq_employees_number UNIQUE (employee_number),
    CONSTRAINT uq_employees_email UNIQUE (email),
    CONSTRAINT uq_employees_user_id UNIQUE (user_id),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_employees_salary_non_negative CHECK (salary >= 0)
);

CREATE INDEX idx_employees_department_id ON employees (department_id);
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_job_title ON employees (job_title);
CREATE INDEX idx_employees_last_name ON employees (last_name);
