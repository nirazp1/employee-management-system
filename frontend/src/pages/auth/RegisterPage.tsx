import { useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth, isApiError } from "../../context/AuthContext";
import { Button, Card, ErrorBanner, Field, Input } from "../../components/ui";

export function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "" });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  function update(field: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) => setForm((f) => ({ ...f, [field]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(form.email, form.password, form.firstName, form.lastName);
      navigate("/", { replace: true });
    } catch (err) {
      setError(isApiError(err) ? err.message : "Something went wrong. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-6">
      <div className="w-full max-w-[420px]">
        <div className="mb-8 text-center">
          <h1 className="font-display text-heading text-deep-ink">Create your account</h1>
          <p className="mt-2 text-body text-slate">
            New accounts start with the Employee role. An admin can be seeded separately.
          </p>
        </div>
        <Card>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {error && <ErrorBanner message={error} />}
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
            <Field label="Password">
              <Input
                type="password"
                required
                minLength={8}
                value={form.password}
                onChange={update("password")}
                placeholder="At least 8 characters"
              />
            </Field>
            <Button type="submit" disabled={submitting} className="mt-2 w-full">
              {submitting ? "Creating account…" : "Create account"}
            </Button>
          </form>
        </Card>
        <p className="mt-6 text-center text-body-sm text-slate">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-deep-ink underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
