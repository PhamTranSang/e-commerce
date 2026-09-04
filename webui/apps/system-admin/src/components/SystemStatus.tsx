import { ExternalLinkIcon } from '@ui';

export function SystemStatus() {
  return (
    <div className="rounded-lg border border-white/10 bg-white/5 p-4">
      <p className="text-xs font-semibold text-white">System Status</p>
      <p className="mt-2.5 flex items-center gap-2 text-xs text-sidebar-foreground">
        <span className="h-2 w-2 rounded-full bg-emerald-500" />
        All systems operational
      </p>
      <p className="mt-1.5 text-[11px] text-sidebar-foreground/70">Last checked: 1 min ago</p>
      <a href="#status" className="mt-2.5 inline-flex items-center gap-1.5 text-xs font-medium text-brand-accent">
        View status page
        <ExternalLinkIcon size={12} />
      </a>
    </div>
  );
}
