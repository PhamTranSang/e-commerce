import { createAuth } from '@shell';

/** Commerce console auth: no role gate — any authenticated account may enter. */
export const { AuthProvider, useAuth, RequireAuth } = createAuth({ accountKey: 'commerceAdminAccount' });
