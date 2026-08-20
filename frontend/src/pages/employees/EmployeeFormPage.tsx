import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api, isApiError } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { DepartmentResponse, EmployeeResponse } from "../../lib/types";
import { Button, Card, ErrorBanner, Field, Input, PageHeader, Select } from "../../components/ui";

interface FormState {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  hireDate: string;
  jobTitle: string;
  salary: string;
  departmentId: string;
  userEmail: string;
}

const EMPTY_FORM: FormState = {
  employeeNumber: "",
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  dateOfBirth: "",
  hireDate: "",
  jobTitle: "",
  salary: "",
  departmentId: "",
  userEmail: "",
};

export function EmployeeFormPage({ mode }: { mode: "create" | "edit" }) {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const { data: departments } = useFetch(
    () => api.getPaged<DepartmentResponse>("/departments", { size: 100 }),
    [],
  );

  const { data: existing } = useFetch(
    () => (mode === "edit" && id ? api.get<EmployeeResponse>(`/employees/${id}`) : Promise.resolve(null)),
    [mode, id],
  );

  useEffect(() => {
    if (existing) {
      setForm({
        employeeNumber: existing.employeeNumber,
        firstName: existing.firstName,
        lastName: existing.lastName,
        email: existing.email,
        phone: existing.phone ?? "",
        dateOfBirth: existing.dateOfBirth ?? "",
        hireDate: existing.hireDate,
        jobTitle: existing.jobTitle,
        salary: String(existing.salary),
        departmentId: existing.department?.id ?? "",
        userEmail: "",
      });
    }
  }, [existing]);

  function update(field: keyof FormState) {
    return (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const payload = {
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        phone: form.phone || null,
        dateOfBirth: form.dateOfBirth || null,
        hireDate: form.hireDate,
        jobTitle: form.jobTitle,
        salary: Number(form.salary),
        departmentId: form.departmentId || null,
      };

      if (mode === "create") {
        const created = await api.post<EmployeeResponse>("/employees", {
          ...payload,
          employeeNumber: form.employeeNumber,
          userEmail: form.userEmail || null,
        });
        navigate(`/employees/${created.id}`);
      } else if (id) {
        await api.put<EmployeeResponse>(`/employees/${id}`, payload);
        navigate(`/employees/${id}`);
      }
    } catch (err) {
      setError(isApiError(err) ? err.message : "Failed to save employee.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={mode === "create" ? "New employee" : "Edit employee"} />
      <Card>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {error && <ErrorBanner message={error} />}

          {mode === "create" && (
            <Field label="Employee number">
              <Input required value={form.employeeNumber} onChange={update("employeeNumber")} />
            </Field>
          )}

          <div className="grid grid-cols-2 gap-4">
            <Field label="First name">
              <Input required value={form.firstName} onChange={update("firstName")} />
            </Field>
            <Field label="Last name">
              <Input required value={form.lastName} onChange={update("lastName")} />
            </Field>
          </div>

          <Field label="Email">
            <Input type="email" required value={form.email} onChange={update("email")} />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Phone">
              <Input value={form.phone} onChange={update("phone")} />
            </Field>
            <Field label="Date of birth">
              <Input type="date" value={form.dateOfBirth} onChange={update("dateOfBirth")} />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Hire date">
              <Input type="date" required value={form.hireDate} onChange={update("hireDate")} />
            </Field>
            <Field label="Job title">
              <Input required value={form.jobTitle} onChange={update("jobTitle")} />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Salary">
              <Input type="number" min="0" step="0.01" required value={form.salary} onChange={update("salary")} />
            </Field>
            <Field label="Department">
              <Select value={form.departmentId} onChange={update("departmentId")}>
                <option value="">No department</option>
                {departments?.data.map((dept) => (
                  <option key={dept.id} value={dept.id}>
                    {dept.name}
                  </option>
                ))}
              </Select>
            </Field>
          </div>

          {mode === "create" && (
            <Field label="Link to existing login (optional)">
              <Input
                type="email"
                placeholder="user@company.com"
                value={form.userEmail}
                onChange={update("userEmail")}
              />
            </Field>
          )}

          <Button type="submit" disabled={submitting} className="mt-2">
            {submitting ? "Saving…" : "Save"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
