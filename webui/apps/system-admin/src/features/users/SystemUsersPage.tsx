import { useEffect, useState } from 'react';
import { Button, Card, CardHeader, CardTitle, Input, Select, Checkbox, Switch, Badge, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, MoreVerticalIcon, PageHeader, Toolbar, StatusBadge, TableFooter, SlideOver, TableState, FieldError, FormError, ConfirmDialog, EMPTY, formatDate } from '@ui';
import type { UserResponse } from '@domain/index';
import { useRoleOptions, useUsers } from '../../api/queries';
import { useCreateUser, useDeactivateUser, useUpdateUser } from '../../api/mutations';
import { parseApiError } from '@api/apiError';

const PAGE_SIZE = 10;

type FormState = { email: string; fullName: string; password: string; roleCodes: string[]; isActive: boolean };
const emptyForm: FormState = { email: '', fullName: '', password: '', roleCodes: [], isActive: true };

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || '?';
}

export function SystemUsersPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('All Status');
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data, isLoading, isError } = useUsers({ page, size: PAGE_SIZE });
  const { data: rolePage } = useRoleOptions();
  const createUser = useCreateUser();
  const updateUser = useUpdateUser();
  const deactivateUser = useDeactivateUser();

  const users = data?.content ?? [];
  const roleOptions = rolePage?.content ?? [];
  const selected = users.find((user) => user.accountId === selectedId) ?? null;
  const isEdit = selected !== null;

  const visible = users
    .filter((user) => status === 'All Status' || (status === 'Active') === user.isActive)
    .filter(
      (user) =>
        user.fullName.toLowerCase().includes(search.toLowerCase()) ||
        user.email.toLowerCase().includes(search.toLowerCase())
    );

  const resetErrors = () => {
    setFormError(null);
    setFieldErrors({});
  };

  const openCreate = () => {
    resetErrors();
    setSelectedId(null);
    setForm(emptyForm);
    setPanelOpen(true);
  };

  const openEdit = (user: UserResponse) => {
    resetErrors();
    setSelectedId(user.accountId);
    setForm({ email: user.email, fullName: user.fullName, password: '', roleCodes: user.roleCodes, isActive: user.isActive });
    setPanelOpen(true);
  };

  useEffect(() => {
    if (selected) {
      setForm({
        email: selected.email,
        fullName: selected.fullName,
        password: '',
        roleCodes: selected.roleCodes,
        isActive: selected.isActive
      });
    }
  }, [selected]);

  const setField = (patch: Partial<FormState>) => setForm((current) => ({ ...current, ...patch }));
  const toggleRole = (code: string) =>
    setForm((current) => ({
      ...current,
      roleCodes: current.roleCodes.includes(code)
        ? current.roleCodes.filter((item) => item !== code)
        : [...current.roleCodes, code]
    }));

  const handleError = (error: unknown) => {
    const parsed = parseApiError(error);
    setFormError(parsed.message);
    setFieldErrors(parsed.fieldErrors);
  };

  const submit = () => {
    resetErrors();
    if (isEdit && selected) {
      updateUser.mutate(
        { accountId: selected.accountId, body: { fullName: form.fullName.trim(), isActive: form.isActive, roleCodes: form.roleCodes } },
        { onError: handleError }
      );
    } else {
      createUser.mutate(
        {
          email: form.email.trim(),
          fullName: form.fullName.trim(),
          password: form.password,
          roleCodes: form.roleCodes,
          isActive: form.isActive
        },
        { onSuccess: () => setPanelOpen(false), onError: handleError }
      );
    }
  };

  const submitDeactivate = () => {
    if (!selected) return;
    deactivateUser.mutate(selected.accountId, {
      onSuccess: () => {
        setConfirmOpen(false);
        setPanelOpen(false);
      },
      onError: (error) => {
        setConfirmOpen(false);
        handleError(error);
      }
    });
  };

  const saving = createUser.isPending || updateUser.isPending;
  const canSubmit = isEdit
    ? Boolean(form.fullName.trim())
    : Boolean(form.email.trim() && form.fullName.trim() && form.password && form.roleCodes.length);

  return (
    <div className="flex h-full overflow-hidden">
      <div className="scroll-slim min-w-0 flex-1 overflow-y-auto px-8 py-7">
        <PageHeader
          title="Users"
          subtitle="Manage platform users and their access"
          actions={
            <Button size="lg" onClick={openCreate}>
              Add User
            </Button>
          }
        />

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>User Directory</CardTitle>
            <Button variant="ghost" size="icon" aria-label="User directory options">
              <MoreVerticalIcon size={16} />
            </Button>
          </CardHeader>
          <div className="px-5 py-4">
            <Toolbar
              search={search}
              onSearchChange={setSearch}
              searchPlaceholder="Search users..."
              filters={[{ id: 'status', value: status, onChange: setStatus, options: ['All Status', 'Active', 'Inactive'] }]}
            />
          </div>
          <Table>
            <TableHeader>
              <tr>
                <TableHead>User</TableHead>
                <TableHead>Roles</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Last Active</TableHead>
                <TableHead>MFA</TableHead>
                <TableHead className="w-9 px-2" />
              </tr>
            </TableHeader>
            <TableBody>
              <TableState colSpan={6} isLoading={isLoading} isError={isError} isEmpty={visible.length === 0} entity="users" />
              {visible.map((user) => (
                <TableRow
                  key={user.accountId}
                  selected={selectedId === user.accountId}
                  onClick={() => openEdit(user)}
                  className="cursor-pointer"
                >
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-soft text-xs font-semibold text-brand">
                        {initials(user.fullName)}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate font-medium text-ink">{user.fullName}</p>
                        <p className="truncate text-xs text-ink-subtle">{user.email}</p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {user.roleCodes.length ? (
                        user.roleCodes.map((code) => (
                          <Badge key={code} tone="info">
                            {code}
                          </Badge>
                        ))
                      ) : (
                        <span className="text-ink-subtle">{EMPTY}</span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <StatusBadge isActive={user.isActive} />
                  </TableCell>
                  <TableCell>{formatDate(user.lastActiveAt)}</TableCell>
                  <TableCell>
                    <Badge tone={user.mfaEnabled ? 'success' : 'neutral'}>{user.mfaEnabled ? 'On' : 'Off'}</Badge>
                  </TableCell>
                  <TableCell className="px-2">
                    <Button variant="ghost" size="icon" className="h-8 w-8" aria-label={`Actions for ${user.fullName}`}>
                      <MoreVerticalIcon size={16} />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TableFooter
            from={visible.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}
            to={(page - 1) * PAGE_SIZE + visible.length}
            total={data?.totalElements ?? 0}
            noun="users"
            page={page}
            totalPages={Math.max(data?.totalPages ?? 1, 1)}
            onPageChange={setPage}
          />
        </Card>
      </div>

      {panelOpen ? (
        <SlideOver
          title={isEdit ? 'Edit User' : 'Add User'}
          subtitle={isEdit ? 'Update user access' : 'Create a new user'}
          onClose={() => setPanelOpen(false)}
          className="w-[420px] shrink-0"
          footer={
            <div className="flex items-center gap-3">
              {isEdit ? (
                <Button
                  variant="outline"
                  className="flex-1"
                  disabled={!selected?.isActive || deactivateUser.isPending}
                  onClick={() => setConfirmOpen(true)}
                >
                  Deactivate
                </Button>
              ) : null}
              <Button className="flex-1" onClick={submit} disabled={!canSubmit || saving}>
                {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create'}
              </Button>
            </div>
          }
        >
          <form className="space-y-5" onSubmit={(event) => { event.preventDefault(); submit(); }}>
            <div>
              <label htmlFor="user-email" className="mb-2 block text-sm text-ink">
                Email <span className="text-red-500">*</span>
              </label>
              <Input
                id="user-email"
                type="email"
                required
                value={form.email}
                disabled={isEdit}
                onChange={(event) => setField({ email: event.target.value })}
                placeholder="name@company.com"
              />
              <FieldError message={fieldErrors.email} />
            </div>

            <div>
              <label htmlFor="user-name" className="mb-2 block text-sm text-ink">
                Full Name <span className="text-red-500">*</span>
              </label>
              <Input
                id="user-name"
                required
                value={form.fullName}
                onChange={(event) => setField({ fullName: event.target.value })}
                placeholder="Enter full name"
              />
              <FieldError message={fieldErrors.fullName} />
            </div>

            {!isEdit ? (
              <div>
                <label htmlFor="user-password" className="mb-2 block text-sm text-ink">
                  Password <span className="text-red-500">*</span>
                </label>
                <Input
                  id="user-password"
                  type="password"
                  required
                  autoComplete="new-password"
                  value={form.password}
                  onChange={(event) => setField({ password: event.target.value })}
                  placeholder="Set an initial password"
                />
                <FieldError message={fieldErrors.password} />
              </div>
            ) : null}

            <div>
              <span className="mb-2 block text-sm text-ink">
                Roles <span className="text-red-500">*</span>
              </span>
              <div className="space-y-2 rounded-lg border border-line p-3">
                {roleOptions.length === 0 ? (
                  <p className="text-xs text-ink-subtle">No roles available.</p>
                ) : (
                  roleOptions.map((role) => (
                    <label key={role.roleId} className="flex items-center gap-2.5 text-sm text-ink">
                      <Checkbox
                        checked={form.roleCodes.includes(role.roleCode)}
                        onChange={() => toggleRole(role.roleCode)}
                      />
                      {role.roleName || role.roleCode}
                      <span className="text-xs text-ink-subtle">({role.roleCode})</span>
                    </label>
                  ))
                )}
              </div>
              <FieldError message={fieldErrors.roleCodes} />
            </div>

            <div>
              <span className="mb-2 block text-sm text-ink">Status</span>
              <Switch
                checked={form.isActive}
                onCheckedChange={(value) => setField({ isActive: value })}
                label={form.isActive ? 'Active' : 'Inactive'}
              />
            </div>

            {isEdit ? <p className="text-xs text-ink-subtle">Email is fixed after creation.</p> : null}

            <FormError message={formError ?? undefined} />
          </form>
        </SlideOver>
      ) : null}

      <ConfirmDialog
        open={confirmOpen}
        title="Deactivate user"
        message={`Deactivate “${selected?.fullName ?? ''}”? They will lose access.`}
        confirmLabel="Deactivate"
        pending={deactivateUser.isPending}
        onConfirm={submitDeactivate}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
