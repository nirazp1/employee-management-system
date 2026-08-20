import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-[1440px] px-4 py-2 text-body-sm font-medium transition-colors ${
    isActive ? "bg-deep-ink text-white" : "text-deep-ink hover:bg-white"
  }`;

export function AppLayout() {
  const { user, logout, hasRole } = useAuth();
  const isManagement = hasRole("ADMIN", "HR", "MANAGER");
  const isPrivileged = hasRole("ADMIN", "HR");

  return (
    <div className="min-h-screen bg-canvas">
      <header className="border-b border-deep-ink/10 bg-soft-meadow">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-4 px-6 py-4">
          <div className="flex items-center gap-6">
            <span className="font-display text-heading-sm text-deep-ink">EMS</span>
            <nav className="flex flex-wrap items-center gap-1">
              {isPrivileged && (
                <NavLink to="/" end className={navItemClass}>
                  Dashboard
                </NavLink>
              )}
              {isManagement && (
                <NavLink to="/employees" className={navItemClass}>
                  Employees
                </NavLink>
              )}
              <NavLink to="/departments" className={navItemClass}>
                Departments
              </NavLink>
              <NavLink to="/attendance" className={navItemClass}>
                Attendance
              </NavLink>
              <NavLink to="/leave" className={navItemClass}>
                Leave
              </NavLink>
              <NavLink to="/payroll" className={navItemClass}>
                Payroll
              </NavLink>
              {user?.employeeId && (
                <NavLink to={`/employees/${user.employeeId}`} className={navItemClass}>
                  My Profile
                </NavLink>
              )}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-body-sm text-slate">
              {user?.firstName} {user?.lastName} · {user?.roles.join(", ")}
            </span>
            <button
              onClick={logout}
              className="rounded-[1440px] border border-deep-ink px-4 py-2 text-body-sm font-medium text-deep-ink hover:bg-white"
            >
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-[1200px] px-6 py-10">
        <Outlet />
      </main>
    </div>
  );
}
