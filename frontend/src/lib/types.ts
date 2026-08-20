// Mirrors the backend's common/response wrappers exactly (common/response/*.java).
// Every request goes through ApiClient, which unwraps these envelopes so the
// rest of the app only ever touches `data`.

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string | null;
}

export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PagedResponse<T> {
  success: boolean;
  data: T[];
  pagination: PaginationMeta;
}

export interface ErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
  };
  timestamp: string;
  path: string;
}

export type RoleName = "ADMIN" | "HR" | "MANAGER" | "EMPLOYEE";

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  roles: RoleName[];
  employeeId: string | null;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}

export type EmployeeStatus = "ACTIVE" | "INACTIVE" | "ON_LEAVE" | "TERMINATED";

export interface DepartmentSummary {
  id: string;
  name: string;
}

export interface EmployeeResponse {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  dateOfBirth: string | null;
  hireDate: string;
  jobTitle: string;
  salary: number;
  status: EmployeeStatus;
  department: DepartmentSummary | null;
  createdAt: string;
  updatedAt: string;
}

export interface ManagerSummary {
  id: string;
  fullName: string;
  jobTitle: string;
}

export interface DepartmentResponse {
  id: string;
  name: string;
  description: string | null;
  manager: ManagerSummary | null;
  createdAt: string;
  updatedAt: string;
}

export type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "HALF_DAY";

export interface AttendanceResponse {
  id: string;
  employeeId: string;
  employeeName: string;
  date: string;
  checkIn: string | null;
  checkOut: string | null;
  status: AttendanceStatus;
  createdAt: string;
  updatedAt: string;
}

export type LeaveType =
  | "SICK"
  | "VACATION"
  | "PERSONAL"
  | "MATERNITY"
  | "PATERNITY"
  | "UNPAID"
  | "OTHER";

export type LeaveStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface LeaveRequestResponse {
  id: string;
  employeeId: string;
  employeeName: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string | null;
  status: LeaveStatus;
  approvedById: string | null;
  approvedByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export type PayrollStatus = "PENDING" | "PROCESSED" | "PAID";

export interface PayrollResponse {
  id: string;
  employeeId: string;
  employeeName: string;
  payPeriodStart: string;
  payPeriodEnd: string;
  baseSalary: number;
  bonuses: number;
  deductions: number;
  netSalary: number;
  paymentDate: string | null;
  status: PayrollStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummaryResponse {
  totalEmployees: number;
  activeEmployees: number;
  inactiveEmployees: number;
  employeesByDepartment: Record<string, number>;
  employeesOnLeave: number;
  todaysAttendance: {
    present: number;
    late: number;
    halfDay: number;
    absent: number;
    totalRecorded: number;
  };
  pendingLeaveRequests: number;
  payrollTotals: {
    pending: number;
    processed: number;
    paid: number;
  };
}
