CREATE TABLE payroll (
    id                  UUID PRIMARY KEY,
    employee_id         UUID NOT NULL,
    pay_period_start    DATE NOT NULL,
    pay_period_end      DATE NOT NULL,
    base_salary         NUMERIC(14, 2) NOT NULL,
    bonuses             NUMERIC(14, 2) NOT NULL DEFAULT 0,
    deductions          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_salary          NUMERIC(14, 2) NOT NULL,
    payment_date        DATE,
    status              VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    CONSTRAINT fk_payroll_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT chk_payroll_period CHECK (pay_period_end >= pay_period_start),
    CONSTRAINT chk_payroll_base_salary_non_negative CHECK (base_salary >= 0),
    CONSTRAINT chk_payroll_bonuses_non_negative CHECK (bonuses >= 0),
    CONSTRAINT chk_payroll_deductions_non_negative CHECK (deductions >= 0),
    CONSTRAINT chk_payroll_net_salary_non_negative CHECK (net_salary >= 0)
);

CREATE INDEX idx_payroll_employee_id ON payroll (employee_id);
CREATE INDEX idx_payroll_status ON payroll (status);
