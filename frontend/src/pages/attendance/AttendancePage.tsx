import { useState } from "react";
import { api, isApiError } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { AttendanceResponse } from "../../lib/types";
import { formatDateTime, statusTone, titleCase } from "../../lib/format";
import { Badge, Button, Card, EmptyState, ErrorBanner, PageHeader, Spinner } from "../../components/ui";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";

function AttendanceTable({ rows }: { rows: AttendanceResponse[] }) {
  if (rows.length === 0) return <EmptyState message="No attendance records." />;
  return (
    <table className="w-full text-left text-body-sm">
      <thead>
        <tr className="border-b border-deep-ink/10 text-caption uppercase tracking-[-0.02em] text-slate">
          <th className="px-6 py-4">Employee</th>
          <th className="px-6 py-4">Date</th>
          <th className="px-6 py-4">Check in</th>
          <th className="px-6 py-4">Check out</th>
          <th className="px-6 py-4">Status</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.id} className="border-b border-deep-ink/10 last:border-0">
            <td className="px-6 py-4 font-medium text-deep-ink">{row.employeeName}</td>
            <td className="px-6 py-4">{row.date}</td>
            <td className="px-6 py-4">{formatDateTime(row.checkIn)}</td>
            <td className="px-6 py-4">{formatDateTime(row.checkOut)}</td>
            <td className="px-6 py-4">
              <Badge tone={statusTone(row.status)}>{titleCase(row.status)}</Badge>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function SelfAttendanceCard({ employeeId }: { employeeId: string }) {
  const [actionError, setActionError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(0);

  const { data, loading, error, refetch } = useFetch(
    () => api.getPaged<AttendanceResponse>(`/employees/${employeeId}/attendance`, { page, size: 5, sort: "date,desc" }),
    [employeeId, page],
  );

  const today = new Date().toISOString().slice(0, 10);
  const todayRecord = data?.data.find((row) => row.date === today);

  async function handleCheckIn() {
    setSubmitting(true);
    setActionError(null);
    try {
      await api.post("/attendance/check-in");
      refetch();
    } catch (err) {
      setActionError(isApiError(err) ? err.message : "Check-in failed.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCheckOut() {
    setSubmitting(true);
    setActionError(null);
    try {
      await api.post("/attendance/check-out");
      refetch();
    } catch (err) {
      setActionError(isApiError(err) ? err.message : "Check-out failed.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="mb-8">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-4">
        <h2 className="font-display text-heading-sm text-deep-ink">My attendance</h2>
        <div className="flex gap-2">
          <Button onClick={handleCheckIn} disabled={submitting || !!todayRecord?.checkIn}>
            Check in
          </Button>
          <Button
            variant="secondary"
            onClick={handleCheckOut}
            disabled={submitting || !todayRecord?.checkIn || !!todayRecord?.checkOut}
          >
            Check out
          </Button>
        </div>
      </div>
      {actionError && <ErrorBanner message={actionError} />}
      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && (
        <>
          <div className="overflow-x-auto">
            <AttendanceTable rows={data.data} />
          </div>
          <Pagination meta={data.pagination} onPageChange={setPage} />
        </>
      )}
    </Card>
  );
}

function TeamAttendanceCard() {
  const [page, setPage] = useState(0);
  const { data, loading, error } = useFetch(
    () => api.getPaged<AttendanceResponse>("/attendance", { page, size: 10 }),
    [page],
  );

  return (
    <Card>
      <h2 className="mb-4 font-display text-heading-sm text-deep-ink">Team attendance</h2>
      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && (
        <>
          <div className="overflow-x-auto">
            <AttendanceTable rows={data.data} />
          </div>
          <Pagination meta={data.pagination} onPageChange={setPage} />
        </>
      )}
    </Card>
  );
}

export function AttendancePage() {
  const { user, hasRole } = useAuth();
  const canViewTeam = hasRole("ADMIN", "HR", "MANAGER");

  return (
    <div>
      <PageHeader title="Attendance" subtitle="Check in/out and review attendance history" />
      {user?.employeeId && <SelfAttendanceCard employeeId={user.employeeId} />}
      {!user?.employeeId && (
        <p className="mb-8 text-body-sm text-slate">
          Your account isn't linked to an employee record, so there's no self check-in available to you.
        </p>
      )}
      {canViewTeam && <TeamAttendanceCard />}
    </div>
  );
}
