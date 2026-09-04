import type { ReactNode } from 'react';
import { cn } from '../../lib/utils';
import { ChevronRightIcon } from '../ui/icons';

type PageHeaderProps = {
  title: string;
  subtitle?: string;
  breadcrumbs?: string[];
  actions?: ReactNode;
  className?: string;
};

export function PageHeader({ title, subtitle, breadcrumbs, actions, className }: PageHeaderProps) {
  return (
    <div className={cn('mb-6', className)}>
      {breadcrumbs?.length ? (
        <nav aria-label="Breadcrumb" className="mb-2 flex items-center gap-1.5 text-sm text-ink-subtle">
          {breadcrumbs.map((crumb, index) => (
            <span key={crumb} className="flex items-center gap-1.5">
              {index > 0 ? <ChevronRightIcon size={13} /> : null}
              <span className={index === breadcrumbs.length - 1 ? 'text-ink-muted' : undefined}>{crumb}</span>
            </span>
          ))}
        </nav>
      ) : null}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold tracking-tight text-ink">{title}</h1>
          {subtitle ? <p className="mt-1 text-sm text-ink-muted">{subtitle}</p> : null}
        </div>
        {actions ? <div className="flex items-center gap-2">{actions}</div> : null}
      </div>
    </div>
  );
}
