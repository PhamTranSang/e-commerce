/** Roles allowed into the System Admin console. Extend as the backend adds roles. */
export const ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN', 'SECURITY_ADMIN', 'OPS_ADMIN'] as const;

export function hasAdminAccess(roles: string[]): boolean {
  return roles.some((role) => (ADMIN_ROLES as readonly string[]).includes(role));
}
