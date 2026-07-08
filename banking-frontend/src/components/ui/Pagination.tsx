interface PaginationProps {
  page: number; // 0-based
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const canPrev = page > 0;
  const canNext = page < totalPages - 1;

  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="secondary"
        disabled={!canPrev}
        onClick={() => onChange(page - 1)}
        aria-label="Previous page"
      >
        ← Prev
      </button>
      <span className="pagination-status">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="secondary"
        disabled={!canNext}
        onClick={() => onChange(page + 1)}
        aria-label="Next page"
      >
        Next →
      </button>
    </nav>
  );
}
