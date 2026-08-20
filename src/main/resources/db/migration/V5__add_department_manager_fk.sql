ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager FOREIGN KEY (manager_id) REFERENCES employees (id) ON DELETE SET NULL;

CREATE INDEX idx_departments_manager_id ON departments (manager_id);
