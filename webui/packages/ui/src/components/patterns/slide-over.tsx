import type { ReactNode } from 'react';
import { cn } from '../../lib/utils';
import { XIcon } from '../ui/icons';

type SlideOverProps = {
  title: string;
  subtitle?: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  className?: string;
};

/**
 * Right-hand detail/creation panel (New Product, Add SKU, Selected Brand).
 * It is docked in the page grid on wide screens rather than overlaying content,
 * matching the mockups.
 */
export function SlideOver({ title, subtitle, onClose, children, footer, className }: SlideOverProps) {
  return (
    <aside
      className={cn('flex h-full min-h-0 flex-col border-l border-line bg-white shadow-panel', className)}
      aria-label={title}
    >
      <div className="flex items-start justify-between gap-3 px-6 pb-4 pt-6">
        <div>
          <h2 className="text-lg font-semibold text-ink">{title}</h2>
          {subtitle ? <p className="mt-0.5 text-sm text-ink-subtle">{subtitle}</p> : null}
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label={`Close ${title}`}
          className="rounded-md p-1 text-ink-subtle transition-colors hover:bg-surface-muted hover:text-ink"
        >
          <XIcon size={18} />
        </button>
      </div>
      <div className="scroll-slim min-h-0 flex-1 overflow-y-auto px-6 pb-6">{children}</div>
      {footer ? <div className="border-t border-line px-6 py-4">{footer}</div> : null}
    </aside>
  );
}
