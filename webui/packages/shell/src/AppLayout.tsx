import { useState, type ReactNode } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar, type SidebarBrand } from './Sidebar';
import { Topbar } from './Topbar';
import type { AppRoute } from './routes';

type AppLayoutProps = {
  brand: SidebarBrand;
  routes: AppRoute[];
  searchPlaceholder: string;
  account: { fullName: string; email?: string } | null;
  onSignOut: () => void;
  sectionLabel?: string;
  sidebarFooter?: ReactNode;
};

/** The shared admin shell: collapsible sidebar + topbar + routed content. */
export function AppLayout({
  brand,
  routes,
  searchPlaceholder,
  account,
  onSignOut,
  sectionLabel,
  sidebarFooter
}: AppLayoutProps) {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <div className="flex h-screen overflow-hidden bg-surface">
      <Sidebar brand={brand} routes={routes} collapsed={collapsed} sectionLabel={sectionLabel} footer={sidebarFooter} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar
          searchPlaceholder={searchPlaceholder}
          account={account}
          onSignOut={onSignOut}
          onToggleSidebar={() => setCollapsed((value) => !value)}
        />
        <main className="scroll-slim min-h-0 flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
