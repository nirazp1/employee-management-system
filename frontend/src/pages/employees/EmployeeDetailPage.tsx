import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, isApiError } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { EmployeeResponse, EmployeeStatus } from "../../lib/types";
import { formatCurrency, formatDate, statusTone, titleCase } from "../../lib/format";
import { Badge, Button, Card, ErrorBanner, PageHeader, Select, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";

const STATUS_OPTIONS: EmployeeStatus[] = ["ACTIVE", "INACTIVE", "ON_LEAVE", "TERMINATED"];

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between border-b border-deep-ink/10 py-3 text-body-sm last:border-0">
      <span className="text-slate">{label}</span>
      <span className="font-medium text-deep-ink">{value}</span>
    </div>
  );
}

export function EmployeeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { hasRole } = useAuth();
  const canManage = hasRole("ADMIN", "HR");
  const navigate = useNavigate();
  const [statusUpdating, setStatusUpdating] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);

  const { data: employee, loading, error, refetch } = useFetch(
    () => api.get<EmployeeResponse>(`/employees/${id}`),
    [id],
  );

  async function handleStatusChange(status: EmployeeStatus) {
    if (!id) return;
    setStatusUpdating(true);
    setStatusError(null);
    try {
      await api.patch(`/employees/${id}/status`, { status });
      refetch();
    } catch (err) {
      setStatusError(isApiError(err) ? err.message : "Failed to update status.");
    } finally {
      setStatusUpdating(false);
    }
  }

  async function handleDelete() {
    if (!id || !window.confirm("Delete this employee? This cannot be undone.")) return;
    try {
      await api.delete(`/employees/${id}`);
      navigate("/employees");
    } catch (err) {
      setStatusError(isApiError(err) ? err.message : "Failed to delete employee.");
    }
  }

  if (loading) return <Spinner />;
  if (error) return <ErrorBanner message={error} />;
  if (!employee) return null;

  return (
    <div>
      <PageHeader
        title={`${employee.firstName} ${employee.lastName}`}
        subtitle={employee.employeeNumber}
        action={
          canManage && (
            <div className="flex gap-2">
              <Link to={`/employees/${employee.id}/edit`}>
                <Button variant="secondary">Edit</Button>
              </Link>
              <Button variant="danger" onClick={handleDelete}>
                Delete
              </Button>
            </div>
          )
        }
      />

      {statusError && <ErrorBanner message={statusError} />}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="mb-2 font-display text-heading-sm text-deep-ink">Profile</h2>
          <Row label="Email" value={employee.email} />
          <Row label="Phone" value={employee.phone ?? "—"} />
          <Row label="Date of birth" value={formatDate(employee.dateOfBirth)} />
          <Row label="Hire date" value={formatDate(employee.hireDate)} />
          <Row label="Job title" value={employee.jobTitle} />
          <Row label="Salary" value={formatCurrency(employee.salary)} />
          <Row label="Department" value={employee.department?.name ?? "—"} />
        </Card>

        <Card>
          <h2 className="mb-4 font-display text-heading-sm text-deep-ink">Status</h2>
          <Badge tone={statusTone(employee.status)}>{titleCase(employee.status)}</Badge>
          {canManage && (
            <div className="mt-4">
              <Select
                value={employee.status}
                disabled={statusUpdating}
                onChange={(e) => handleStatusChange(e.target.value as EmployeeStatus)}
              >
                {STATUS_OPTIONS.map((s) => (
                  <option key={s} value={s}>
                    {titleCase(s)}
                  </option>
                ))}
              </Select>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
