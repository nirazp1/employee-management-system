import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { DepartmentResponse, EmployeeResponse, EmployeeStatus } from "../../lib/types";
import { formatCurrency, statusTone, titleCase } from "../../lib/format";
import { Badge, Button, Card, EmptyState, ErrorBanner, Input, PageHeader, Select, Spinner } from "../../components/ui";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";

const STATUS_OPTIONS: EmployeeStatus[] = ["ACTIVE", "INACTIVE", "ON_LEAVE", "TERMINATED"];

export function EmployeeListPage() {
  const { hasRole } = useAuth();
  const canManage = hasRole("ADMIN", "HR");

  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [departmentId, setDepartmentId] = useState("");
  const [status, setStatus] = useState("");

  const { data: departments } = useFetch(
    () => api.getPaged<DepartmentResponse>("/departments", { size: 100 }),
    [],
  );

  const { data, loading, error } = useFetch(
    () =>
      api.getPaged<EmployeeResponse>("/employees", {
        page,
        size: 10,
        sort: "lastName,asc",
        search: search || undefined,
        departmentId: departmentId || undefined,
        status: status || undefined,
      }),
    [page, search, departmentId, status],
  );

  return (
    <div>
      <PageHeader
        title="Employees"
        subtitle="Search, filter, and manage employee records"
        action={canManage && <Link to="/employees/new"><Button>New employee</Button></Link>}
      />

      <Card className="mb-6 flex flex-wrap gap-4">
        <Input
          placeholder="Search name, email, or number…"
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          className="max-w-xs"
        />
        <Select
          value={departmentId}
          onChange={(e) => {
            setPage(0);
            setDepartmentId(e.target.value);
          }}
          className="max-w-xs"
        >
          <option value="">All departments</option>
          {departments?.data.map((dept) => (
            <option key={dept.id} value={dept.id}>
              {dept.name}
            </option>
          ))}
        </Select>
        <Select
          value={status}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value);
          }}
          className="max-w-xs"
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {titleCase(s)}
            </option>
          ))}
        </Select>
      </Card>

      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && data.data.length === 0 && <EmptyState message="No employees match these filters." />}

      {data && data.data.length > 0 && (
        <Card className="overflow-x-auto p-0">
          <table className="w-full text-left text-body-sm">
            <thead>
              <tr className="border-b border-deep-ink/10 text-caption uppercase tracking-[-0.02em] text-slate">
                <th className="px-6 py-4">Employee</th>
                <th className="px-6 py-4">Job title</th>
                <th className="px-6 py-4">Department</th>
                <th className="px-6 py-4">Salary</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data.data.map((employee) => (
                <tr key={employee.id} className="border-b border-deep-ink/10 last:border-0">
                  <td className="px-6 py-4">
                    <div className="font-medium text-deep-ink">
                      {employee.firstName} {employee.lastName}
                    </div>
                    <div className="text-slate">{employee.email}</div>
                  </td>
                  <td className="px-6 py-4">{employee.jobTitle}</td>
                  <td className="px-6 py-4">{employee.department?.name ?? "—"}</td>
                  <td className="px-6 py-4">{formatCurrency(employee.salary)}</td>
                  <td className="px-6 py-4">
                    <Badge tone={statusTone(employee.status)}>{titleCase(employee.status)}</Badge>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <Link to={`/employees/${employee.id}`} className="text-body-sm font-medium underline">
                        View
                      </Link>
                      {canManage && (
                        <Link to={`/employees/${employee.id}/edit`} className="text-body-sm font-medium underline">
                          Edit
                        </Link>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="px-6 pb-6">
            <Pagination meta={data.pagination} onPageChange={setPage} />
          </div>
        </Card>
      )}
      {!loading && !error && !data && <EmptyState message="Failed to load employees." />}
    </div>
  );
}
