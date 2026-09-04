import type {
  CreateRoleRequest,
  CreateUserRequest,
  PageResponse,
  RoleResponse,
  SystemDashboardStats,
  UpdateRoleRequest,
  UpdateUserRequest,
  UserResponse
} from '@domain/index';
import { http } from './http';

export type AdminPageParams = { page?: number; size?: number };

async function getPage<T>(url: string, params: AdminPageParams): Promise<PageResponse<T>> {
  const { data } = await http.get<PageResponse<T>>(url, {
    params: { page: params.page ?? 1, size: params.size ?? 20 }
  });
  return data;
}

// --- Users ---
export const listUsers = (params: AdminPageParams = {}) => getPage<UserResponse>('/api/admin/users', params);
export const createUser = (body: CreateUserRequest) =>
  http.post<UserResponse>('/api/admin/users', body).then((r) => r.data);
export const updateUser = (accountId: string, body: UpdateUserRequest) =>
  http.patch<UserResponse>(`/api/admin/users/${accountId}`, body).then((r) => r.data);
export const deactivateUser = (accountId: string) =>
  http.delete<void>(`/api/admin/users/${accountId}`).then((r) => r.data);

// --- Roles ---
export const listRoles = (params: AdminPageParams = {}) => getPage<RoleResponse>('/api/admin/roles', params);
export const createRole = (body: CreateRoleRequest) =>
  http.post<RoleResponse>('/api/admin/roles', body).then((r) => r.data);
export const updateRole = (roleId: string, body: UpdateRoleRequest) =>
  http.patch<RoleResponse>(`/api/admin/roles/${roleId}`, body).then((r) => r.data);
export const deactivateRole = (roleId: string) =>
  http.delete<void>(`/api/admin/roles/${roleId}`).then((r) => r.data);

// --- Dashboard ---
export const fetchDashboardStats = () =>
  http.get<SystemDashboardStats>('/api/admin/dashboard/stats').then((r) => r.data);
