import type { AppRoute } from '@shell';
import { CodeIcon, FileTextIcon, SettingsIcon, ShieldCheckIcon, UsersIcon } from '@ui';
import { SystemDashboardPage } from './features/dashboard/SystemDashboardPage';
import { SystemUsersPage } from './features/users/SystemUsersPage';
import { SystemRolesPage } from './features/roles/SystemRolesPage';
import { Placeholder } from './components/Placeholder';

export const routes: AppRoute[] = [
  // Landing dashboard is reachable at '/', but the sidebar starts at Users (per the mockups).
  { path: '/', label: 'Dashboard', element: <SystemDashboardPage />, end: true, hidden: true },
  { path: '/users', label: 'Users', icon: UsersIcon, element: <SystemUsersPage /> },
  { path: '/roles', label: 'Roles', icon: ShieldCheckIcon, element: <SystemRolesPage /> },
  {
    path: '/audit-logs',
    label: 'Audit Logs',
    icon: FileTextIcon,
    element: <Placeholder title="Audit Logs" subtitle="Review system audit events" />
  },
  {
    path: '/api-endpoints',
    label: 'API Endpoints',
    icon: CodeIcon,
    element: <Placeholder title="API Endpoints" subtitle="Manage API endpoints and access" />
  },
  {
    path: '/settings',
    label: 'Settings',
    icon: SettingsIcon,
    element: <Placeholder title="Settings" subtitle="Platform configuration" />
  }
];
