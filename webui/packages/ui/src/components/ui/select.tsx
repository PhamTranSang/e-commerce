import * as React from 'react';
import { cn } from '../../lib/utils';
import { ChevronDownIcon } from './icons';

type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement> & { wrapperClassName?: string };

/** Native select styled to match the mockups (chevron drawn on top). */
export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, wrapperClassName, children, ...props }, ref) => (
    <div className={cn('relative', wrapperClassName)}>
      <select
        ref={ref}
        className={cn(
          'h-10 w-full appearance-none rounded-lg border border-line bg-white pl-2.5 pr-8 text-[13px] text-ink',
          'focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15',
          className
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDownIcon
        size={16}
        className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-ink-subtle"
      />
    </div>
  )
);
Select.displayName = 'Select';
