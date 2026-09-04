import { PageHeader, Card } from '@ui';

/** Route stub for sections not part of the current scope (Audit Logs, API Endpoints, Settings). */
export function Placeholder({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="px-8 py-7">
      <PageHeader title={title} subtitle={subtitle} />
      <Card className="p-10 text-center text-sm text-ink-subtle">
        This section is not implemented yet.
      </Card>
    </div>
  );
}
