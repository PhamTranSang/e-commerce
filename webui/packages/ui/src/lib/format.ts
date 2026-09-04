/** Shown wherever the backend has no field to populate a mockup column yet. */
export const EMPTY = '—';

const dateFormat = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
const dateTimeFormat = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
});

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return EMPTY;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? EMPTY : dateFormat.format(date);
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return EMPTY;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? EMPTY : dateTimeFormat.format(date);
}

export function formatCount(value: number | null | undefined): string {
  return typeof value === 'number' ? value.toLocaleString('en-US') : EMPTY;
}

/** Backend sends BigDecimal as a string; keep it exact and only group the digits. */
export function formatAmount(amount: string | null | undefined): string {
  if (!amount) return EMPTY;
  const parsed = Number(amount);
  if (Number.isNaN(parsed)) return amount;
  return parsed.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export const statusLabel = (isActive: boolean) => (isActive ? 'Active' : 'Inactive');
