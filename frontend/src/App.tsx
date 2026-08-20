import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { AppLayout } from "./components/AppLayout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { HomeRedirect } from "./pages/HomeRedirect";
import { EmployeeListPage } from "./pages/employees/EmployeeListPage";
import { EmployeeDetailPage } from "./pages/employees/EmployeeDetailPage";
import { EmployeeFormPage } from "./pages/employees/EmployeeFormPage";
import { DepartmentListPage } from "./pages/departments/DepartmentListPage";
import { AttendancePage } from "./pages/attendance/AttendancePage";
import { LeavePage } from "./pages/leave/LeavePage";
import { PayrollPage } from "./pages/payroll/PayrollPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomeRedirect />} />

          <Route path="/employees" element={<ProtectedRoute roles={["ADMIN", "HR", "MANAGER"]} />}>
            <Route index element={<EmployeeListPage />} />
            <Route path="new" element={<EmployeeFormPage mode="create" />} />
            <Route path=":id/edit" element={<EmployeeFormPage mode="edit" />} />
          </Route>
          <Route path="/employees/:id" element={<EmployeeDetailPage />} />

          <Route path="/departments" element={<DepartmentListPage />} />
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/leave" element={<LeavePage />} />
          <Route path="/payroll" element={<PayrollPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
