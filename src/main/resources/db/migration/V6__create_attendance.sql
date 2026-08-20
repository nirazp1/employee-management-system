CREATE TABLE attendance (
    id              UUID PRIMARY KEY,
    employee_id     UUID NOT NULL,
    date            DATE NOT NULL,
    check_in        TIMESTAMP,
    check_out       TIMESTAMP,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_employee_date UNIQUE (employee_id, date),
    CONSTRAINT chk_attendance_checkout_after_checkin CHECK (check_out IS NULL OR check_in IS NULL OR check_out >= check_in)
);

CREATE INDEX idx_attendance_employee_id ON attendance (employee_id);
CREATE INDEX idx_attendance_date ON attendance (date);
