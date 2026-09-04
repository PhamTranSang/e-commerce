import type { ReactNode } from 'react';
import { cn } from '../../lib/utils';
import { ArrowDownIcon, ArrowUpIcon } from '../ui/icons';

export type StatTone = 'emerald' | 'sky' | 'amber' | 'indigo' | 'teal' | 'red';

export const statToneClass: Record<StatTone, string> = {
  emerald: 'bg-emerald-50 text-emerald-600',
  sky: 'bg-sky-50 text-sky-600',
  amber: 'bg-amber-50 text-amber-600',
  indigo: 'bg-indigo-50 text-indigo-600',
  teal: 'bg-teal-50 text-teal-600',
  red: 'bg-red-50 text-red-500'
};

type StatCardProps = {
  icon: ReactNode;
  label: string;
  value: string;
  tone?: StatTone;
  caption?: string;
  delta?: string;
  direction?: 'up' | 'down';
  className?: string;
};

export function StatCard({
  icon,
  label,
  value,
  tone = 'emerald',
  caption,
  delta,
  direction = 'up',
  className
}: StatCardProps) {
  return (
    <div className={cn('flex items-center gap-3 rounded-xl border border-line bg-white p-4 shadow-card', className)}>
      <span className={cn('flex h-11 w-11 shrink-0 items-center justify-center rounded-full', statToneClass[tone])}>
        {icon}
      </span>
      <div className="min-w-0">
        <p className="truncate text-sm text-ink-muted">{label}</p>
        <p className="mt-0.5 text-2xl font-bold tracking-tight text-ink">{value}</p>
        {delta ? (
          <p
            className={cn(
              'mt-1 flex items-center gap-1 whitespace-nowrap text-[11px] font-medium',
              direction === 'up' ? 'text-emerald-600' : 'text-red-500'
            )}
          >
            {direction === 'up' ? <ArrowUpIcon size={12} /> : <ArrowDownIcon size={12} />}
            {delta} vs last 7 days
          </p>
        ) : null}
        {caption ? <p className="mt-1 whitespace-nowrap text-[11px] text-ink-subtle">{caption}</p> : null}
      </div>
    </div>
  );
}
