/**
 * Contract for the System Admin console. These endpoints are NOT implemented on the
 * backend yet — the FE is written against this contract (see docs/system-admin-api-contract.md).
 * Shapes follow the existing conventions: PageResponse<T>, UUID→string, Instant→ISO-8601.
 */

export type RoleType = 'SYSTEM' | 'ADMIN' | 'STANDARD' | 'READ_ONLY' | 'CUSTOM';
export type PermissionLevel = 'ALLOWED' | 'LIMITED' | 'DENIED';

// GET /api/admin/users?page&size — one row of the user directory.
export type UserResponse = {
  accountId: string;
  email: string;
  fullName: string;
  isActive: boolean;
  roleCodes: string[];
  mfaEnabled: boolean;
  lastActiveAt: string | null;
  createdAt: string;
  updatedAt: string;
};

// POST /api/admin/users
export type CreateUserRequest = {
  email: string;
  fullName: string;
  password: string;
  roleCodes: string[];
  isActive?: boolean;
};

// PATCH /api/admin/users/{accountId}
export type UpdateUserRequest = {
  fullName: string;
  isActive: boolean;
  roleCodes: string[];
};

// GET /api/admin/roles?page&size
export type RoleResponse = {
  roleId: string;
  roleCode: string;
  roleName: string;
  description: string | null;
  type: RoleType;
  isActive: boolean;
  userCount: number;
  /** capability key → level; optional, drives the permission matrix. */
  permissions: Record<string, PermissionLevel> | null;
  createdAt: string;
  updatedAt: string;
};

// POST /api/admin/roles
export type CreateRoleRequest = {
  roleCode: string;
  roleName: string;
  description?: string;
  type: RoleType;
};

// PATCH /api/admin/roles/{roleId}
export type UpdateRoleRequest = {
  roleName: string;
  description?: string;
  type: RoleType;
  isActive?: boolean;
};

// GET /api/admin/dashboard/stats
export type SystemDashboardStats = {
  activeUsers: number;
  roleCount: number;
  recentAuditEvents: number;
  failedLogins: number;
  endpointHealthPercent: number;
};
