import { useState, type FormEvent } from "react";
import { api, isApiError } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { EmployeeResponse, PayrollResponse, PayrollStatus } from "../../lib/types";
import { formatCurrency, formatDate, statusTone, titleCase } from "../../lib/format";
import { Badge, Button, Card, EmptyState, ErrorBanner, Field, Input, PageHeader, Select, Spinner } from "../../components/ui";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";

const STATUS_OPTIONS: PayrollStatus[] = ["PENDING", "PROCESSED", "PAID"];

interface PayrollForm {
  employeeId: string;
  payPeriodStart: string;
  payPeriodEnd: string;
  baseSalary: string;
  bonuses: string;
  deductions: string;
  paymentDate: string;
  status: PayrollStatus;
}

const EMPTY_FORM: PayrollForm = {
  employeeId: "",
  payPeriodStart: "",
  payPeriodEnd: "",
  baseSalary: "",
  bonuses: "0",
  deductions: "0",
  paymentDate: "",
  status: "PENDING",
};

function PayrollTable({
  rows,
  onStatusChange,
}: {
  rows: PayrollResponse[];
  onStatusChange?: (row: PayrollResponse, status: PayrollStatus) => void;
}) {
  if (rows.length === 0) return <EmptyState message="No payroll records." />;
  return (
    <table className="w-full text-left text-body-sm">
      <thead>
        <tr className="border-b border-deep-ink/10 text-caption uppercase tracking-[-0.02em] text-slate">
          <th className="px-6 py-4">Employee</th>
          <th className="px-6 py-4">Period</th>
          <th className="px-6 py-4">Base</th>
          <th className="px-6 py-4">Bonuses</th>
          <th className="px-6 py-4">Deductions</th>
          <th className="px-6 py-4">Net</th>
          <th className="px-6 py-4">Status</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.id} className="border-b border-deep-ink/10 last:border-0">
            <td className="px-6 py-4 font-medium text-deep-ink">{row.employeeName}</td>
            <td className="px-6 py-4">
              {formatDate(row.payPeriodStart)} – {formatDate(row.payPeriodEnd)}
            </td>
            <td className="px-6 py-4">{formatCurrency(row.baseSalary)}</td>
            <td className="px-6 py-4">{formatCurrency(row.bonuses)}</td>
            <td className="px-6 py-4">{formatCurrency(row.deductions)}</td>
            <td className="px-6 py-4 font-medium">{formatCurrency(row.netSalary)}</td>
            <td className="px-6 py-4">
              {onStatusChange ? (
                <Select
                  value={row.status}
                  className="w-auto py-2 text-body-sm"
                  onChange={(e) => onStatusChange(row, e.target.value as PayrollStatus)}
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s} value={s}>
                      {titleCase(s)}
                    </option>
                  ))}
                </Select>
              ) : (
                <Badge tone={statusTone(row.status)}>{titleCase(row.status)}</Badge>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function NewPayrollForm({ employees, onCreated }: { employees: EmployeeResponse[]; onCreated: () => void }) {
  const [form, setForm] = useState<PayrollForm>(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function update<K extends keyof PayrollForm>(field: K) {
    return (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await api.post("/payroll", {
        employeeId: form.employeeId,
        payPeriodStart: form.payPeriodStart,
        payPeriodEnd: form.payPeriodEnd,
        baseSalary: Number(form.baseSalary),
        bonuses: Number(form.bonuses || 0),
        deductions: Number(form.deductions || 0),
        paymentDate: form.paymentDate || null,
      });
      setForm(EMPTY_FORM);
      onCreated();
    } catch (err) {
      setError(isApiError(err) ? err.message : "Failed to create payroll record.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="mb-8">
      <h2 className="mb-4 font-display text-heading-sm text-deep-ink">New payroll record</h2>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <ErrorBanner message={error} />}
        <Field label="Employee">
          <Select required value={form.employeeId} onChange={update("employeeId")}>
            <option value="">Select an employee</option>
            {employees.map((emp) => (
              <option key={emp.id} value={emp.id}>
                {emp.firstName} {emp.lastName} ({emp.employeeNumber})
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Pay period start">
            <Input type="date" required value={form.payPeriodStart} onChange={update("payPeriodStart")} />
          </Field>
          <Field label="Pay period end">
            <Input type="date" required value={form.payPeriodEnd} onChange={update("payPeriodEnd")} />
          </Field>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <Field label="Base salary">
            <Input type="number" min="0" step="0.01" required value={form.baseSalary} onChange={update("baseSalary")} />
          </Field>
          <Field label="Bonuses">
            <Input type="number" min="0" step="0.01" value={form.bonuses} onChange={update("bonuses")} />
          </Field>
          <Field label="Deductions">
            <Input type="number" min="0" step="0.01" value={form.deductions} onChange={update("deductions")} />
          </Field>
        </div>
        <Field label="Payment date (optional)">
          <Input type="date" value={form.paymentDate} onChange={update("paymentDate")} />
        </Field>
        <Button type="submit" disabled={submitting} className="self-start">
          {submitting ? "Saving…" : "Create record"}
        </Button>
      </form>
    </Card>
  );
}

function PrivilegedPayrollView() {
  const [page, setPage] = useState(0);
  const [statusError, setStatusError] = useState<string | null>(null);

  const { data, loading, error, refetch } = useFetch(
    () => api.getPaged<PayrollResponse>("/payroll", { page, size: 10 }),
    [page],
  );
  const { data: employeePage } = useFetch(() => api.getPaged<EmployeeResponse>("/employees", { size: 100 }), []);

  // PUT /payroll/{id} replaces the whole record (there's no PATCH-status
  // endpoint on the backend), so a status-only edit still has to resend
  // every other field back exactly as it already was.
  async function handleStatusChange(row: PayrollResponse, status: PayrollStatus) {
    setStatusError(null);
    try {
      await api.put(`/payroll/${row.id}`, {
        payPeriodStart: row.payPeriodStart,
        payPeriodEnd: row.payPeriodEnd,
        baseSalary: row.baseSalary,
        bonuses: row.bonuses,
        deductions: row.deductions,
        paymentDate: row.paymentDate,
        status,
      });
      refetch();
    } catch (err) {
      setStatusError(isApiError(err) ? err.message : "Failed to update status.");
    }
  }

  return (
    <div>
      <NewPayrollForm employees={employeePage?.data ?? []} onCreated={refetch} />
      {statusError && <ErrorBanner message={statusError} />}
      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && (
        <Card className="overflow-x-auto p-0">
          <PayrollTable rows={data.data} onStatusChange={handleStatusChange} />
          <div className="px-6 pb-6">
            <Pagination meta={data.pagination} onPageChange={setPage} />
          </div>
        </Card>
      )}
    </div>
  );
}

function OwnPayrollView({ employeeId }: { employeeId: string }) {
  const [page, setPage] = useState(0);
  const { data, loading, error } = useFetch(
    () => api.getPaged<PayrollResponse>(`/employees/${employeeId}/payroll`, { page, size: 10 }),
    [employeeId, page],
  );

  return (
    <div>
      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && data.data.length === 0 && <EmptyState message="No payroll records yet." />}
      {data && data.data.length > 0 && (
        <Card className="overflow-x-auto p-0">
          <PayrollTable rows={data.data} />
          <div className="px-6 pb-6">
            <Pagination meta={data.pagination} onPageChange={setPage} />
          </div>
        </Card>
      )}
    </div>
  );
}

export function PayrollPage() {
  const { user, hasRole } = useAuth();
  const isPrivileged = hasRole("ADMIN", "HR");

  return (
    <div>
      <PageHeader
        title="Payroll"
        subtitle={isPrivileged ? "Manage payroll records for all employees" : "Your payroll history"}
      />
      {isPrivileged && <PrivilegedPayrollView />}
      {!isPrivileged && user?.employeeId && <OwnPayrollView employeeId={user.employeeId} />}
      {!isPrivileged && !user?.employeeId && (
        <p className="text-body-sm text-slate">
          Your account isn't linked to an employee record, so there's no payroll history to show.
        </p>
      )}
    </div>
  );
}
