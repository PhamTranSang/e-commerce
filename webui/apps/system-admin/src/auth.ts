import { createAuth } from '@shell';
import { hasAdminAccess } from './auth-roles';

/** Thrown by signIn when credentials are valid but the account lacks System Admin access. */
export class AccessDeniedError extends Error {
  constructor() {
    super('Your account does not have System Admin access.');
    this.name = 'AccessDeniedError';
  }
}

export const { AuthProvider, useAuth, RequireAuth } = createAuth({
  accountKey: 'systemAdminAccount',
  hasAccess: hasAdminAccess,
  deniedError: () => new AccessDeniedError()
});
