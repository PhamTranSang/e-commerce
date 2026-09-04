import { useQuery } from '@tanstack/react-query';
import { fetchDashboardStats, listRoles, listUsers, type AdminPageParams } from '@api';

export const adminKeys = {
  users: (params: AdminPageParams) => ['users', params] as const,
  roles: (params: AdminPageParams) => ['roles', params] as const,
  dashboardStats: () => ['dashboard', 'stats'] as const
};

export const useUsers = (params: AdminPageParams) =>
  useQuery({ queryKey: adminKeys.users(params), queryFn: () => listUsers(params) });

export const useRoles = (params: AdminPageParams) =>
  useQuery({ queryKey: adminKeys.roles(params), queryFn: () => listRoles(params) });

export const useDashboardStats = () =>
  useQuery({ queryKey: adminKeys.dashboardStats(), queryFn: fetchDashboardStats });

/** Role codes for the create/edit user role picker. */
export const useRoleOptions = () => useRoles({ page: 1, size: 200 });