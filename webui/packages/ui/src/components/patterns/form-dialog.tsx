import type { ReactNode } from 'react';
import { Button } from '../ui/button';
import { XIcon } from '../ui/icons';

type FormDialogProps = {
  open: boolean;
  title: string;
  children: ReactNode;
  submitLabel?: string;
  cancelLabel?: string;
  pending?: boolean;
  canSubmit?: boolean;
  onSubmit: () => void;
  onCancel: () => void;
};

/** Centered modal wrapping a small create/edit form. */
export function FormDialog({
  open,
  title,
  children,
  submitLabel = 'Save',
  cancelLabel = 'Cancel',
  pending = false,
  canSubmit = true,
  onSubmit,
  onCancel
}: FormDialogProps) {
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4"
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onClick={onCancel}
    >
      <form
        className="w-full max-w-md rounded-xl border border-line bg-white shadow-panel"
        onClick={(event) => event.stopPropagation()}
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit();
        }}
      >
        <div className="flex items-center justify-between border-b border-line px-6 py-4">
          <h2 className="text-base font-semibold text-ink">{title}</h2>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Close"
            className="rounded-md p-1 text-ink-subtle hover:bg-surface-muted hover:text-ink"
          >
            <XIcon size={18} />
          </button>
        </div>
        <div className="space-y-4 px-6 py-5">{children}</div>
        <div className="flex justify-end gap-3 border-t border-line px-6 py-4">
          <Button type="button" variant="outline" onClick={onCancel} disabled={pending}>
            {cancelLabel}
          </Button>
          <Button type="submit" disabled={pending || !canSubmit}>
            {pending ? 'Saving…' : submitLabel}
          </Button>
        </div>
      </form>
    </div>
  );
}
