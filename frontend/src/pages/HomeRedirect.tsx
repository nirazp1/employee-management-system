import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { DashboardPage } from "./dashboard/DashboardPage";

// "/" can't be a single fixed page: the dashboard summary endpoint is
// ADMIN/HR-only on the backend, so routing everyone there would either 403
// or (worse, if nested under a role-gated ProtectedRoute) redirect-loop back
// to "/". Instead we pick a sensible landing page per role right here.
export function HomeRedirect() {
  const { hasRole, user } = useAuth();

  if (hasRole("ADMIN", "HR")) {
    return <DashboardPage />;
  }
  if (hasRole("MANAGER")) {
    return <Navigate to="/employees" replace />;
  }
  if (user?.employeeId) {
    return <Navigate to={`/employees/${user.employeeId}`} replace />;
  }
  return <Navigate to="/departments" replace />;
}
