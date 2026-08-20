import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { RoleName } from "../lib/types";
import { Spinner } from "./ui";

export function ProtectedRoute({ roles }: { roles?: RoleName[] }) {
  const { user, loading, hasRole } = useAuth();

  if (loading) {
    return <Spinner />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !hasRole(...roles)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
