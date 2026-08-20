export function formatDate(value: string | null): string {
  if (!value) return "—";
  // The backend sends bare LocalDate strings ("2026-01-15", no time/zone).
  // Per the JS spec, `new Date("2026-01-15")` is parsed as UTC midnight, but
  // `new Date("2026-01-15T00:00")` (a date-*time* string, which is what
  // formatDateTime below deals with) is parsed as local midnight - so this
  // one form silently shifts a day backwards in any timezone behind UTC.
  // Building the Date from numeric parts instead always uses local time.
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString();
}

export function formatDateTime(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString();
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

export function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

// Every module has some notion of "good / needs attention / bad" - mapping
// them all onto the same three-tone badge scale keeps status colors
// consistent across Employee/Attendance/Leave/Payroll without introducing
// new colors outside the approved UI palette (no green/red - just the
// yellow/dark-ink/neutral scale used everywhere else).
export type BadgeTone = "neutral" | "positive" | "attention" | "strong";

const STATUS_TONES: Record<string, BadgeTone> = {
  ACTIVE: "positive",
  ON_LEAVE: "attention",
  INACTIVE: "neutral",
  TERMINATED: "strong",
  PRESENT: "positive",
  LATE: "attention",
  HALF_DAY: "attention",
  ABSENT: "strong",
  PENDING: "attention",
  APPROVED: "positive",
  REJECTED: "strong",
  CANCELLED: "neutral",
  PROCESSED: "positive",
  PAID: "positive",
};

export function statusTone(status: string): BadgeTone {
  return STATUS_TONES[status] ?? "neutral";
}
