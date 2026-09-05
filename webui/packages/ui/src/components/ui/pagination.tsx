import { cn } from '../../lib/utils';
import { ChevronLeftIcon, ChevronRightIcon } from './icons';

type PaginationProps = {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
};

/** Builds the `1 2 3 4 5 … 250` window seen in the mockups. */
function buildPages(page: number, totalPages: number): (number | 'gap')[] {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i + 1);
  const head = Array.from({ length: 5 }, (_, i) => i + 1);
  if (page > 5) {
    const around = [page - 1, page, page + 1].filter((p) => p > 1 && p < totalPages);
    return [1, 'gap', ...around, 'gap', totalPages];
  }
  return [...head, 'gap', totalPages];
}

export function Pagination({ page, totalPages, onPageChange, className }: PaginationProps) {
  const items = buildPages(page, totalPages);
  const navButton =
    'flex h-8 min-w-8 items-center justify-center rounded-md border border-line bg-white px-2 text-ink-muted ' +
    'transition-colors hover:bg-surface-muted disabled:opacity-40 disabled:hover:bg-white';

  return (
    <nav className={cn('flex items-center gap-1.5', className)} aria-label="Pagination">
      <button type="button" className={navButton} disabled={page <= 1} onClick={() => onPageChange(page - 1)} aria-label="Previous page">
        <ChevronLeftIcon size={15} />
      </button>
      {items.map((item, index) =>
        item === 'gap' ? (
          <span key={`gap-${index}`} className="px-1.5 text-sm text-ink-subtle">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            aria-current={item === page ? 'page' : undefined}
            onClick={() => onPageChange(item)}
            className={cn(
              'flex h-8 min-w-8 items-center justify-center rounded-md px-2 text-sm transition-colors',
              item === page
                ? 'bg-brand font-semibold text-white'
                : 'border border-transparent text-ink-muted hover:bg-surface-muted'
            )}
          >
            {item}
          </button>
        )
      )}
      <button type="button" className={navButton} disabled={page >= totalPages} onClick={() => onPageChange(page + 1)} aria-label="Next page">
        <ChevronRightIcon size={15} />
      </button>
    </nav>
  );
}
