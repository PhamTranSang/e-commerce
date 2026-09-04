import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createRole,
  createUser,
  deactivateRole,
  deactivateUser,
  updateRole,
  updateUser
} from '@api';
import type { CreateRoleRequest, CreateUserRequest, UpdateRoleRequest, UpdateUserRequest } from '@domain';

function useAdminMutation<TArgs, TResult>(
  mutationFn: (args: TArgs) => Promise<TResult>,
  invalidate: readonly (readonly unknown[])[]
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      for (const key of invalidate) queryClient.invalidateQueries({ queryKey: key as unknown[] });
    }
  });
}

const USERS = [['users'], ['dashboard']] as const;
const ROLES = [['roles'], ['dashboard']] as const;

export const useCreateUser = () => useAdminMutation((body: CreateUserRequest) => createUser(body), USERS);
export const useUpdateUser = () =>
  useAdminMutation((args: { accountId: string; body: UpdateUserRequest }) => updateUser(args.accountId, args.body), USERS);
export const useDeactivateUser = () => useAdminMutation((accountId: string) => deactivateUser(accountId), USERS);

export const useCreateRole = () => useAdminMutation((body: CreateRoleRequest) => createRole(body), ROLES);
export const useUpdateRole = () =>
  useAdminMutation((args: { roleId: string; body: UpdateRoleRequest }) => updateRole(args.roleId, args.body), ROLES);
export const useDeactivateRole = () => useAdminMutation((roleId: string) => deactivateRole(roleId), ROLES);