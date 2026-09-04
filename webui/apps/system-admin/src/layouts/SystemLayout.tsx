import { useNavigate } from 'react-router-dom';
import { AppLayout } from '@shell';
import { ShieldCheckIcon } from '@ui';
import { useAuth } from '../auth';
import { routes } from '../routes';
import { SystemStatus } from '../components/SystemStatus';

export function SystemLayout() {
  const { account, signOut } = useAuth();
  const navigate = useNavigate();
  return (
    <AppLayout
      brand={{ icon: ShieldCheckIcon, label: 'System Admin' }}
      routes={routes}
      sectionLabel="Main"
      sidebarFooter={<SystemStatus />}
      searchPlaceholder="Search users, roles, endpoints, IPs..."
      account={account}
      onSignOut={() => {
        signOut();
        navigate('/login', { replace: true });
      }}
    />
  );
}
