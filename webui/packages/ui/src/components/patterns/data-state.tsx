import { TableCell, TableRow } from '../ui/table';

type TableStateProps = {
  colSpan: number;
  isLoading: boolean;
  isError: boolean;
  isEmpty: boolean;
  entity: string;
};

/**
 * Single row covering the loading / error / empty states of a table body.
 * Returns null once there are rows to render.
 */
export function TableState({ colSpan, isLoading, isError, isEmpty, entity }: TableStateProps) {
  if (!isLoading && !isError && !isEmpty) return null;

  const message = isLoading
    ? `Loading ${entity}…`
    : isError
      ? `Could not load ${entity}. Check that the API is reachable.`
      : `No ${entity} yet.`;

  return (
    <TableRow className="hover:bg-transparent">
      <TableCell colSpan={colSpan} className="py-10 text-center text-sm text-ink-subtle">
        {message}
      </TableCell>
    </TableRow>
  );
}

/** Marks a panel the API cannot populate yet, instead of inventing numbers. */
export function NotBackedYet({ what }: { what: string }) {
  return <p className="py-6 text-center text-sm text-ink-subtle">{what} is not exposed by the API yet.</p>;
}
