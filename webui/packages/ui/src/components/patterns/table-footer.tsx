import { Pagination } from '../ui/pagination';

type TableFooterProps = {
  from: number;
  to: number;
  total: number;
  noun: string;
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
};

export function TableFooter({ from, to, total, noun, page, totalPages, onPageChange }: TableFooterProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
      <p className="text-sm text-ink-muted">
        Showing {from} to {to} of {total.toLocaleString('en-US')} {noun}
      </p>
      <Pagination page={page} totalPages={totalPages} onPageChange={onPageChange} />
    </div>
  );
}
