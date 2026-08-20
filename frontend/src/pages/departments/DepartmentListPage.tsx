import { useState, type FormEvent } from "react";
import { api, isApiError } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { DepartmentResponse, EmployeeResponse } from "../../lib/types";
import { Button, Card, EmptyState, ErrorBanner, Field, Input, PageHeader, Select, Spinner } from "../../components/ui";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";

interface DeptForm {
  name: string;
  description: string;
  managerId: string;
}

const EMPTY_FORM: DeptForm = { name: "", description: "", managerId: "" };

function DepartmentForm({
  initial,
  employees,
  onCancel,
  onSubmit,
}: {
  initial: DeptForm;
  employees: EmployeeResponse[];
  onCancel: () => void;
  onSubmit: (form: DeptForm) => Promise<void>;
}) {
  const [form, setForm] = useState<DeptForm>(initial);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit(form);
    } catch (err) {
      setError(isApiError(err) ? err.message : "Failed to save department.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {error && <ErrorBanner message={error} />}
      <Field label="Name">
        <Input required value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
      </Field>
      <Field label="Description">
        <Input
          value={form.description}
          onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
        />
      </Field>
      <Field label="Manager">
        <Select value={form.managerId} onChange={(e) => setForm((f) => ({ ...f, managerId: e.target.value }))}>
          <option value="">No manager</option>
          {employees.map((emp) => (
            <option key={emp.id} value={emp.id}>
              {emp.firstName} {emp.lastName}
            </option>
          ))}
        </Select>
      </Field>
      <div className="flex gap-2">
        <Button type="submit" disabled={submitting}>
          {submitting ? "Saving…" : "Save"}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

export function DepartmentListPage() {
  const { hasRole } = useAuth();
  const canManage = hasRole("ADMIN", "HR");
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);

  const { data, loading, error, refetch } = useFetch(
    () => api.getPaged<DepartmentResponse>("/departments", { page, size: 10 }),
    [page],
  );

  const { data: employeePage } = useFetch(
    () => (canManage ? api.getPaged<EmployeeResponse>("/employees", { size: 100 }) : Promise.resolve(null)),
    [canManage],
  );
  const employees = employeePage?.data ?? [];

  async function handleCreate(form: DeptForm) {
    await api.post("/departments", {
      name: form.name,
      description: form.description || null,
      managerId: form.managerId || null,
    });
    setCreating(false);
    refetch();
  }

  async function handleUpdate(id: string, form: DeptForm) {
    await api.put(`/departments/${id}`, {
      name: form.name,
      description: form.description || null,
      managerId: form.managerId || null,
    });
    setEditingId(null);
    refetch();
  }

  async function handleDelete(id: string) {
    if (!window.confirm("Delete this department?")) return;
    await api.delete(`/departments/${id}`);
    refetch();
  }

  return (
    <div>
      <PageHeader
        title="Departments"
        subtitle="Organizational units and their managers"
        action={canManage && !creating && <Button onClick={() => setCreating(true)}>New department</Button>}
      />

      {creating && (
        <Card className="mb-6">
          <h2 className="mb-4 font-display text-heading-sm text-deep-ink">New department</h2>
          <DepartmentForm
            initial={EMPTY_FORM}
            employees={employees}
            onCancel={() => setCreating(false)}
            onSubmit={handleCreate}
          />
        </Card>
      )}

      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && data.data.length === 0 && <EmptyState message="No departments yet." />}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {data?.data.map((dept) => (
          <Card key={dept.id}>
            {editingId === dept.id ? (
              <DepartmentForm
                initial={{
                  name: dept.name,
                  description: dept.description ?? "",
                  managerId: dept.manager?.id ?? "",
                }}
                employees={employees}
                onCancel={() => setEditingId(null)}
                onSubmit={(form) => handleUpdate(dept.id, form)}
              />
            ) : (
              <>
                <h2 className="font-display text-heading-sm text-deep-ink">{dept.name}</h2>
                {dept.description && <p className="mt-2 text-body-sm text-slate">{dept.description}</p>}
                <p className="mt-3 text-body-sm">
                  <span className="text-slate">Manager: </span>
                  <span className="font-medium">{dept.manager?.fullName ?? "Unassigned"}</span>
                </p>
                {canManage && (
                  <div className="mt-4 flex gap-2">
                    <Button variant="secondary" className="px-4 py-2" onClick={() => setEditingId(dept.id)}>
                      Edit
                    </Button>
                    <Button variant="danger" className="px-4 py-2" onClick={() => handleDelete(dept.id)}>
                      Delete
                    </Button>
                  </div>
                )}
              </>
            )}
          </Card>
        ))}
      </div>

      {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
    </div>
  );
}
