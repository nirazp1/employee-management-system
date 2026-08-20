import { useState, type FormEvent } from "react";
import { api, isApiError } from "../../lib/api";
import { useFetch } from "../../lib/useFetch";
import type { LeaveRequestResponse, LeaveType } from "../../lib/types";
import { formatDate, statusTone, titleCase } from "../../lib/format";
import { Badge, Button, Card, EmptyState, ErrorBanner, Field, Input, PageHeader, Select, Spinner } from "../../components/ui";
import { Pagination } from "../../components/Pagination";
import { useAuth } from "../../context/AuthContext";

const LEAVE_TYPES: LeaveType[] = ["SICK", "VACATION", "PERSONAL", "MATERNITY", "PATERNITY", "UNPAID", "OTHER"];

function NewLeaveRequestForm({ onCreated }: { onCreated: () => void }) {
  const [leaveType, setLeaveType] = useState<LeaveType>("VACATION");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await api.post("/leave-requests", { leaveType, startDate, endDate, reason: reason || null });
      setStartDate("");
      setEndDate("");
      setReason("");
      onCreated();
    } catch (err) {
      setError(isApiError(err) ? err.message : "Failed to submit leave request.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="mb-8">
      <h2 className="mb-4 font-display text-heading-sm text-deep-ink">Request leave</h2>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <ErrorBanner message={error} />}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Field label="Type">
            <Select value={leaveType} onChange={(e) => setLeaveType(e.target.value as LeaveType)}>
              {LEAVE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {titleCase(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Start date">
            <Input type="date" required value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </Field>
          <Field label="End date">
            <Input type="date" required value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </Field>
        </div>
        <Field label="Reason (optional)">
          <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Briefly explain the request" />
        </Field>
        <Button type="submit" disabled={submitting} className="self-start">
          {submitting ? "Submitting…" : "Submit request"}
        </Button>
      </form>
    </Card>
  );
}

export function LeavePage() {
  const { user, hasRole } = useAuth();
  const canDecide = hasRole("ADMIN", "HR", "MANAGER");
  const [page, setPage] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data, loading, error, refetch } = useFetch(
    () => api.getPaged<LeaveRequestResponse>("/leave-requests", { page, size: 10, sort: "createdAt,desc" }),
    [page],
  );

  async function act(action: "approve" | "reject" | "cancel", id: string) {
    setActionError(null);
    try {
      await api.put(`/leave-requests/${id}/${action}`);
      refetch();
    } catch (err) {
      setActionError(isApiError(err) ? err.message : `Failed to ${action} the request.`);
    }
  }

  return (
    <div>
      <PageHeader title="Leave requests" subtitle="Submit and track time-off requests" />

      {user?.employeeId ? (
        <NewLeaveRequestForm onCreated={refetch} />
      ) : (
        <p className="mb-8 text-body-sm text-slate">
          Your account isn't linked to an employee record, so you can't submit a leave request.
        </p>
      )}

      {actionError && <ErrorBanner message={actionError} />}
      {loading && <Spinner />}
      {error && <ErrorBanner message={error} />}
      {data && data.data.length === 0 && <EmptyState message="No leave requests to show." />}

      {data && data.data.length > 0 && (
        <Card className="overflow-x-auto p-0">
          <table className="w-full text-left text-body-sm">
            <thead>
              <tr className="border-b border-deep-ink/10 text-caption uppercase tracking-[-0.02em] text-slate">
                <th className="px-6 py-4">Employee</th>
                <th className="px-6 py-4">Type</th>
                <th className="px-6 py-4">Dates</th>
                <th className="px-6 py-4">Reason</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4" />
              </tr>
            </thead>
            <tbody>
              {data.data.map((req) => {
                const isOwn = req.employeeId === user?.employeeId;
                const canCancel = req.status === "PENDING" && (isOwn || canDecide);
                const canApproveReject = req.status === "PENDING" && canDecide;
                return (
                  <tr key={req.id} className="border-b border-deep-ink/10 last:border-0">
                    <td className="px-6 py-4 font-medium text-deep-ink">{req.employeeName}</td>
                    <td className="px-6 py-4">{titleCase(req.leaveType)}</td>
                    <td className="px-6 py-4">
                      {formatDate(req.startDate)} – {formatDate(req.endDate)}
                    </td>
                    <td className="px-6 py-4 text-slate">{req.reason ?? "—"}</td>
                    <td className="px-6 py-4">
                      <Badge tone={statusTone(req.status)}>{titleCase(req.status)}</Badge>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex justify-end gap-2">
                        {canApproveReject && (
                          <>
                            <button
                              className="text-body-sm font-medium underline"
                              onClick={() => act("approve", req.id)}
                            >
                              Approve
                            </button>
                            <button
                              className="text-body-sm font-medium underline"
                              onClick={() => act("reject", req.id)}
                            >
                              Reject
                            </button>
                          </>
                        )}
                        {canCancel && (
                          <button className="text-body-sm font-medium underline" onClick={() => act("cancel", req.id)}>
                            Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <div className="px-6 pb-6">
            <Pagination meta={data.pagination} onPageChange={setPage} />
          </div>
        </Card>
      )}
    </div>
  );
}
