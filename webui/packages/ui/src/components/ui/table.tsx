import * as React from 'react';
import { cn } from '../../lib/utils';

export function Table({ className, ...props }: React.TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-x-auto">
      <table className={cn('w-full border-collapse text-sm', className)} {...props} />
    </div>
  );
}

export function TableHeader({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn('border-y border-line bg-surface-muted/60', className)} {...props} />;
}

export function TableBody({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn('divide-y divide-line', className)} {...props} />;
}

type RowProps = React.HTMLAttributes<HTMLTableRowElement> & { selected?: boolean };

export function TableRow({ className, selected, ...props }: RowProps) {
  return (
    <tr
      className={cn('transition-colors hover:bg-surface-muted/70', selected && 'bg-brand-soft/60', className)}
      {...props}
    />
  );
}

export function TableHead({ className, ...props }: React.ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn('whitespace-nowrap px-3.5 py-3 text-left text-xs font-semibold text-ink-muted', className)}
      {...props}
    />
  );
}

export function TableCell({ className, ...props }: React.TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn('px-3.5 py-3.5 align-middle text-[13px] text-ink-muted', className)} {...props} />;
}
