CREATE TABLE leave_requests (
    id              UUID PRIMARY KEY,
    employee_id     UUID NOT NULL,
    leave_type      VARCHAR(20) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    reason          VARCHAR(1000),
    status          VARCHAR(20) NOT NULL,
    approved_by     UUID,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_requests_approved_by FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_requests_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_leave_requests_employee_id ON leave_requests (employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests (status);
