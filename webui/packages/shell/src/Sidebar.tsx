import type { ComponentType, ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { cn, ChevronDownIcon, type IconProps } from '@ui';
import { navItems, type AppRoute } from './routes';

export type SidebarBrand = { icon: ComponentType<IconProps>; label: string };

type SidebarProps = {
  brand: SidebarBrand;
  routes: AppRoute[];
  collapsed?: boolean;
  /** Small uppercase label above the nav (e.g. "MAIN"). */
  sectionLabel?: string;
  /** Content pinned to the bottom (e.g. a system-status card). */
  footer?: ReactNode;
};

export function SidebarBrandHeader({
  brand,
  collapsed = false,
  className
}: {
  brand: SidebarBrand;
  collapsed?: boolean;
  className?: string;
}) {
  const Icon = brand.icon;
  return (
    <div className={cn('flex items-center gap-2.5 px-5 py-6', collapsed && 'justify-center px-0', className)}>
      <Icon size={28} className="text-brand-accent" />
      {collapsed ? null : <span className="whitespace-nowrap text-[17px] font-bold text-white">{brand.label}</span>}
    </div>
  );
}

export function Sidebar({ brand, routes, collapsed = false, sectionLabel, footer }: SidebarProps) {
  const items = navItems(routes);
  return (
    <aside
      className={cn(
        'flex h-full shrink-0 flex-col bg-sidebar transition-[width] duration-200',
        collapsed ? 'w-[76px]' : 'w-[246px]'
      )}
    >
      <SidebarBrandHeader brand={brand} collapsed={collapsed} />
      {sectionLabel && !collapsed ? (
        <p className="px-6 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-sidebar-foreground/60">
          {sectionLabel}
        </p>
      ) : null}
      <nav className={cn('space-y-1', collapsed ? 'px-3' : 'px-3.5')}>
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              title={collapsed ? item.label : undefined}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg py-3 text-sm transition-colors',
                  collapsed ? 'justify-center px-0' : 'px-3.5',
                  isActive
                    ? 'bg-sidebar-active font-medium text-white'
                    : 'text-sidebar-foreground hover:bg-white/5 hover:text-white'
                )
              }
            >
              {Icon ? <Icon size={19} /> : null}
              {collapsed ? null : (
                <>
                  <span className="flex-1">{item.label}</span>
                  {item.expandable ? <ChevronDownIcon size={15} className="opacity-70" /> : null}
                </>
              )}
            </NavLink>
          );
        })}
      </nav>
      {footer && !collapsed ? <div className="mt-auto m-3.5">{footer}</div> : null}
    </aside>
  );
}
