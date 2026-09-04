import type { ComponentType, ReactNode } from 'react';
import { Route } from 'react-router-dom';
import type { IconProps } from '@ui';

/**
 * Single source of truth for a console's navigable pages: it drives both the router
 * (`renderRoutes`) and the sidebar (`navItems`), so a page is declared once.
 */
export type AppRoute = {
  path: string;
  label: string;
  element: ReactNode;
  icon?: ComponentType<IconProps>;
  /** Exact-match highlighting for the index route. */
  end?: boolean;
  /** Show a disclosure chevron in the sidebar (e.g. a section with children). */
  expandable?: boolean;
  /** Hide from the sidebar while still routing (default: shown). */
  hidden?: boolean;
};

export function renderRoutes(routes: AppRoute[]) {
  return routes.map((route) => <Route key={route.path} path={route.path} element={route.element} />);
}

export function navItems(routes: AppRoute[]) {
  return routes.filter((route) => !route.hidden);
}
