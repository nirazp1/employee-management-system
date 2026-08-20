import { api } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { DashboardSummaryResponse } from "../../lib/types";
import { Card, ErrorBanner, PageHeader, Spinner } from "../../components/ui";

function StatCard({ label, value }: { label: string; value: number | string }) {
  return (
    <Card className="flex flex-col gap-1">
      <span className="text-caption font-medium uppercase tracking-[-0.02em] text-slate">{label}</span>
      <span className="font-display text-heading text-deep-ink">{value}</span>
    </Card>
  );
}

function currency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

export function DashboardPage() {
  const { data, loading, error } = useFetch(() => api.get<DashboardSummaryResponse>("/dashboard/summary"), []);

  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Organization-wide snapshot" />
      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && (
        <div className="flex flex-col gap-8">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <StatCard label="Total employees" value={data.totalEmployees} />
            <StatCard label="Active" value={data.activeEmployees} />
            <StatCard label="Inactive" value={data.inactiveEmployees} />
            <StatCard label="On leave" value={data.employeesOnLeave} />
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <h2 className="mb-4 font-display text-heading-sm text-deep-ink">Employees by department</h2>
              {Object.keys(data.employeesByDepartment).length === 0 ? (
                <p className="text-body-sm text-slate">No departments have employees yet.</p>
              ) : (
                <ul className="flex flex-col gap-2">
                  {Object.entries(data.employeesByDepartment).map(([name, count]) => (
                    <li key={name} className="flex items-center justify-between border-b border-deep-ink/10 pb-2 text-body-sm">
                      <span>{name}</span>
                      <span className="font-medium">{count}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card>
              <h2 className="mb-4 font-display text-heading-sm text-deep-ink">Today's attendance</h2>
              <ul className="flex flex-col gap-2 text-body-sm">
                <li className="flex justify-between border-b border-deep-ink/10 pb-2">
                  <span>Present</span>
                  <span className="font-medium">{data.todaysAttendance.present}</span>
                </li>
                <li className="flex justify-between border-b border-deep-ink/10 pb-2">
                  <span>Late</span>
                  <span className="font-medium">{data.todaysAttendance.late}</span>
                </li>
                <li className="flex justify-between border-b border-deep-ink/10 pb-2">
                  <span>Half day</span>
                  <span className="font-medium">{data.todaysAttendance.halfDay}</span>
                </li>
                <li className="flex justify-between border-b border-deep-ink/10 pb-2">
                  <span>Absent (inferred)</span>
                  <span className="font-medium">{data.todaysAttendance.absent}</span>
                </li>
                <li className="flex justify-between pb-2">
                  <span>Total recorded</span>
                  <span className="font-medium">{data.todaysAttendance.totalRecorded}</span>
                </li>
              </ul>
            </Card>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card>
              <h2 className="mb-2 font-display text-heading-sm text-deep-ink">Pending leave requests</h2>
              <p className="font-display text-heading text-deep-ink">{data.pendingLeaveRequests}</p>
            </Card>
            <Card>
              <h2 className="mb-4 font-display text-heading-sm text-deep-ink">Payroll totals</h2>
              <ul className="flex flex-col gap-2 text-body-sm">
                <li className="flex justify-between border-b border-deep-ink/10 pb-2">
                  <span>Pending</span>
                  <span className="font-medium">{currency(data.payrollTotals.pending)}</span>
                </li>
                <li className="flex justify-between border-b border-deep-ink/10 pb-2">
                  <span>Processed</span>
                  <span className="font-medium">{currency(data.payrollTotals.processed)}</span>
                </li>
                <li className="flex justify-between pb-2">
                  <span>Paid</span>
                  <span className="font-medium">{currency(data.payrollTotals.paid)}</span>
                </li>
              </ul>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
