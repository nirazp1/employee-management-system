import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { isApiError } from "../../context/AuthContext";
import { Button, Card, ErrorBanner, Field, Input } from "../../components/ui";

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      const redirectTo = (location.state as { from?: string } | null)?.from ?? "/";
      navigate(redirectTo, { replace: true });
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
          <h1 className="font-display text-heading text-deep-ink">EMS</h1>
          <p className="mt-2 text-body text-slate">Sign in to the Employee Management System</p>
        </div>
        <Card>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {error && <ErrorBanner message={error} />}
            <Field label="Email">
              <Input
                type="email"
                required
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
              />
            </Field>
            <Field label="Password">
              <Input
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </Field>
            <Button type="submit" disabled={submitting} className="mt-2 w-full">
              {submitting ? "Signing in…" : "Log in"}
            </Button>
          </form>
        </Card>
        <p className="mt-6 text-center text-body-sm text-slate">
          Don't have an account?{" "}
          <Link to="/register" className="font-medium text-deep-ink underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
