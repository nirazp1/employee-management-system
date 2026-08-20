import type { PaginationMeta } from "../lib/types";
import { Button } from "./ui";

export function Pagination({
  meta,
  onPageChange,
}: {
  meta: PaginationMeta;
  onPageChange: (page: number) => void;
}) {
  if (meta.totalPages <= 1) return null;

  return (
    <div className="mt-6 flex items-center justify-between text-body-sm text-slate">
      <span>
        Page {meta.page + 1} of {meta.totalPages} · {meta.totalElements} total
      </span>
      <div className="flex gap-2">
        <Button
          type="button"
          variant="ghost"
          className="px-4 py-2"
          disabled={meta.page <= 0}
          onClick={() => onPageChange(meta.page - 1)}
        >
          Previous
        </Button>
        <Button
          type="button"
          variant="ghost"
          className="px-4 py-2"
          disabled={meta.page >= meta.totalPages - 1}
          onClick={() => onPageChange(meta.page + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
}
