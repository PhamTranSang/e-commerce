import * as React from 'react';
import { cn } from '../../lib/utils';

export const Checkbox = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      type="checkbox"
      className={cn(
        'h-4 w-4 cursor-pointer rounded border-line text-brand accent-[var(--color-brand)]',
        'focus:outline-none focus:ring-2 focus:ring-brand/25',
        className
      )}
      {...props}
    />
  )
);
Checkbox.displayName = 'Checkbox';
