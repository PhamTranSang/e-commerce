import { useNavigate } from 'react-router-dom';
import { AppLayout } from '@shell';
import { ShoppingBagIcon } from '@ui';
import { useAuth } from '../auth';
import { routes } from '../routes';

export function CommerceLayout() {
  const { account, signOut } = useAuth();
  const navigate = useNavigate();
  return (
    <AppLayout
      brand={{ icon: ShoppingBagIcon, label: 'Commerce Admin' }}
      routes={routes}
      searchPlaceholder="Search products, SKUs, categories, brands..."
      account={account}
      onSignOut={() => {
        signOut();
        navigate('/login', { replace: true });
      }}
    />
  );
}
