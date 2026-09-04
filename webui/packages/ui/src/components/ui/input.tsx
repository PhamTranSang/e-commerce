import * as React from 'react';
import { cn } from '../../lib/utils';

export const inputBase =
  'h-10 w-full rounded-lg border border-line bg-white px-3 text-sm text-ink transition-colors ' +
  'placeholder:text-ink-subtle focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 ' +
  'disabled:cursor-not-allowed disabled:bg-surface-muted';

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => <input ref={ref} className={cn(inputBase, className)} {...props} />
);
Input.displayName = 'Input';

type IconInputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  icon?: React.ReactNode;
  trailing?: React.ReactNode;
  wrapperClassName?: string;
};

/** Input with a leading icon and/or a trailing adornment (search, password reveal…). */
export const IconInput = React.forwardRef<HTMLInputElement, IconInputProps>(
  ({ className, wrapperClassName, icon, trailing, ...props }, ref) => (
    <div className={cn('relative', wrapperClassName)}>
      {icon ? (
        <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-subtle">{icon}</span>
      ) : null}
      <input
        ref={ref}
        className={cn(inputBase, icon && 'pl-10', trailing && 'pr-10', className)}
        {...props}
      />
      {trailing ? <span className="absolute right-2.5 top-1/2 -translate-y-1/2">{trailing}</span> : null}
    </div>
  )
);
IconInput.displayName = 'IconInput';
