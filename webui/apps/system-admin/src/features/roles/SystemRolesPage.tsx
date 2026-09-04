import { useMemo, useState } from 'react';
import { Button, Card, CardHeader, CardTitle, Input, Textarea, Select, Switch, Badge, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, CheckIcon, MoreVerticalIcon, PencilIcon, ShieldCheckIcon, UsersIcon, XCircleIcon, PageHeader, StatCard, Toolbar, StatusBadge, TableFooter, TableState, NotBackedYet, FieldError, FormError, ConfirmDialog, FormDialog, EMPTY, formatDateTime } from '@ui';
import type { PermissionLevel, RoleResponse, RoleType } from '@domain/index';
import { useRoles } from '../../api/queries';
import { useCreateRole, useDeactivateRole, useUpdateRole } from '../../api/mutations';
import { parseApiError } from '@api/apiError';

const PAGE_SIZE = 8;
const ROLE_TYPES: RoleType[] = ['SYSTEM', 'ADMIN', 'STANDARD', 'READ_ONLY', 'CUSTOM'];
const typeLabel: Record<RoleType, string> = {
  SYSTEM: 'System',
  ADMIN: 'Admin',
  STANDARD: 'Standard',
  READ_ONLY: 'Read-Only',
  CUSTOM: 'Custom'
};

function permissionMark(level: PermissionLevel) {
  if (level === 'ALLOWED') return <CheckIcon size={16} className="text-emerald-600" />;
  if (level === 'LIMITED') return <span className="text-amber-500">—</span>;
  return <XCircleIcon size={15} className="text-red-500" />;
}

export function SystemRolesPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('All Status');
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({ roleCode: '', roleName: '', description: '', type: 'CUSTOM' as RoleType });
  const [createError, setCreateError] = useState<string | null>(null);
  const [createFieldErrors, setCreateFieldErrors] = useState<Record<string, string>>({});

  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({ roleName: '', description: '', type: 'CUSTOM' as RoleType, isActive: true });
  const [editError, setEditError] = useState<string | null>(null);
  const [editFieldErrors, setEditFieldErrors] = useState<Record<string, string>>({});

  const [confirmOpen, setConfirmOpen] = useState(false);

  // Role counts are small; fetch once and paginate client-side so the stat cards stay accurate.
  const { data, isLoading, isError } = useRoles({ page: 1, size: 200 });
  const createRole = useCreateRole();
  const updateRole = useUpdateRole();
  const deactivateRole = useDeactivateRole();

  const allRoles = useMemo(() => data?.content ?? [], [data]);
  const selected = allRoles.find((role) => role.roleId === selectedId) ?? null;

  const filtered = allRoles
    .filter((role) => status === 'All Status' || (status === 'Active') === role.isActive)
    .filter(
      (role) =>
        role.roleName.toLowerCase().includes(search.toLowerCase()) ||
        role.roleCode.toLowerCase().includes(search.toLowerCase())
    );
  const totalPages = Math.max(Math.ceil(filtered.length / PAGE_SIZE), 1);
  const pageRoles = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const stats = {
    total: allRoles.length,
    active: allRoles.filter((role) => role.isActive).length,
    admin: allRoles.filter((role) => role.type === 'ADMIN' || role.type === 'SYSTEM').length,
    restricted: allRoles.filter((role) => Object.values(role.permissions ?? {}).includes('DENIED')).length
  };

  const openEdit = (role: RoleResponse) => {
    setEditForm({ roleName: role.roleName, description: role.description ?? '', type: role.type, isActive: role.isActive });
    setEditError(null);
    setEditFieldErrors({});
    setEditOpen(true);
  };

  const submitCreate = () => {
    setCreateError(null);
    setCreateFieldErrors({});
    createRole.mutate(
      {
        roleCode: createForm.roleCode.trim(),
        roleName: createForm.roleName.trim(),
        description: createForm.description.trim() || undefined,
        type: createForm.type
      },
      {
        onSuccess: () => {
          setCreateOpen(false);
          setCreateForm({ roleCode: '', roleName: '', description: '', type: 'CUSTOM' });
        },
        onError: (error) => {
          const parsed = parseApiError(error);
          setCreateError(parsed.message);
          setCreateFieldErrors(parsed.fieldErrors);
        }
      }
    );
  };

  const submitEdit = () => {
    if (!selected) return;
    setEditError(null);
    setEditFieldErrors({});
    updateRole.mutate(
      {
        roleId: selected.roleId,
        body: {
          roleName: editForm.roleName.trim(),
          description: editForm.description.trim() || undefined,
          type: editForm.type,
          isActive: editForm.isActive
        }
      },
      {
        onSuccess: () => setEditOpen(false),
        onError: (error) => {
          const parsed = parseApiError(error);
          setEditError(parsed.message);
          setEditFieldErrors(parsed.fieldErrors);
        }
      }
    );
  };

  const submitDeactivate = () => {
    if (!selected) return;
    deactivateRole.mutate(selected.roleId, {
      onSuccess: () => {
        setConfirmOpen(false);
        setSelectedId(null);
      },
      onError: () => setConfirmOpen(false)
    });
  };

  const permissions = selected?.permissions ? Object.entries(selected.permissions) : [];

  return (
    <div className="px-8 py-7">
      <PageHeader
        title="Roles"
        subtitle="Manage roles and their permissions"
        actions={
          <Button size="lg" onClick={() => setCreateOpen(true)}>
            Create Role
          </Button>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon={<ShieldCheckIcon size={20} />} label="Total Roles" value={String(stats.total)} tone="sky" />
        <StatCard icon={<UsersIcon size={20} />} label="Active Roles" value={String(stats.active)} tone="emerald" />
        <StatCard icon={<ShieldCheckIcon size={20} />} label="Admin Roles" value={String(stats.admin)} tone="indigo" />
        <StatCard icon={<XCircleIcon size={20} />} label="Restricted Permissions" value={String(stats.restricted)} tone="red" />
      </div>

      <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,520px)]">
        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Role Catalog</CardTitle>
          </CardHeader>
          <div className="px-5 py-4">
            <Toolbar
              search={search}
              onSearchChange={(value) => {
                setSearch(value);
                setPage(1);
              }}
              searchPlaceholder="Search roles..."
              filters={[{ id: 'status', value: status, onChange: setStatus, options: ['All Status', 'Active', 'Inactive'] }]}
            />
          </div>
          <Table>
            <TableHeader>
              <tr>
                <TableHead>Role Name</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Users</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Last Updated</TableHead>
                <TableHead className="w-9 px-2" />
              </tr>
            </TableHeader>
            <TableBody>
              <TableState colSpan={7} isLoading={isLoading} isError={isError} isEmpty={pageRoles.length === 0} entity="roles" />
              {pageRoles.map((role) => (
                <TableRow
                  key={role.roleId}
                  selected={selectedId === role.roleId}
                  onClick={() => setSelectedId(role.roleId)}
                  className="cursor-pointer"
                >
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-ink">{role.roleName}</span>
                      {role.type === 'SYSTEM' ? <Badge tone="brand">System</Badge> : null}
                    </div>
                  </TableCell>
                  <TableCell className="max-w-[220px] truncate">{role.description ?? EMPTY}</TableCell>
                  <TableCell>{role.userCount}</TableCell>
                  <TableCell>
                    <Badge tone="neutral">{typeLabel[role.type]}</Badge>
                  </TableCell>
                  <TableCell>
                    <StatusBadge isActive={role.isActive} />
                  </TableCell>
                  <TableCell className="whitespace-nowrap">{formatDateTime(role.updatedAt)}</TableCell>
                  <TableCell className="px-2">
                    <Button variant="ghost" size="icon" className="h-8 w-8" aria-label={`Actions for ${role.roleName}`}>
                      <MoreVerticalIcon size={16} />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TableFooter
            from={pageRoles.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}
            to={(page - 1) * PAGE_SIZE + pageRoles.length}
            total={filtered.length}
            noun="roles"
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
          />
        </Card>

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Selected Role</CardTitle>
            {selected ? (
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" onClick={() => openEdit(selected)}>
                  <PencilIcon size={14} />
                  Edit
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  disabled={!selected.isActive || deactivateRole.isPending}
                  onClick={() => setConfirmOpen(true)}
                >
                  <XCircleIcon size={14} />
                  Deactivate
                </Button>
              </div>
            ) : null}
          </CardHeader>

          {selected ? (
            <div className="p-5">
              <div className="flex items-start gap-4">
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-soft text-brand">
                  <ShieldCheckIcon size={22} />
                </span>
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-lg font-bold text-ink">{selected.roleName}</p>
                    {selected.type === 'SYSTEM' ? <Badge tone="brand">System</Badge> : null}
                  </div>
                  <p className="mt-1 text-sm text-ink-muted">{selected.description ?? 'No description.'}</p>
                </div>
              </div>

              <dl className="mt-5 grid grid-cols-4 gap-4 border-y border-line py-4 text-sm">
                <div>
                  <dt className="text-ink-subtle">Type</dt>
                  <dd className="mt-1 text-ink">{typeLabel[selected.type]}</dd>
                </div>
                <div>
                  <dt className="text-ink-subtle">Users</dt>
                  <dd className="mt-1 text-ink">{selected.userCount}</dd>
                </div>
                <div>
                  <dt className="text-ink-subtle">Status</dt>
                  <dd className="mt-1">
                    <StatusBadge isActive={selected.isActive} />
                  </dd>
                </div>
                <div>
                  <dt className="text-ink-subtle">Code</dt>
                  <dd className="mt-1 font-mono text-xs text-ink">{selected.roleCode}</dd>
                </div>
              </dl>

              <p className="mb-3 mt-5 text-sm font-semibold text-ink">Permission Matrix</p>
              {permissions.length === 0 ? (
                <NotBackedYet what="This role's permission set" />
              ) : (
                <ul className="divide-y divide-line rounded-lg border border-line">
                  {permissions.map(([capability, level]) => (
                    <li key={capability} className="flex items-center justify-between px-4 py-2.5 text-sm">
                      <span className="text-ink">{capability}</span>
                      {permissionMark(level)}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ) : (
            <p className="p-5 text-sm text-ink-subtle">Select a role to see its details.</p>
          )}
        </Card>
      </div>

      <FormDialog
        open={createOpen}
        title="Create Role"
        onCancel={() => setCreateOpen(false)}
        onSubmit={submitCreate}
        submitLabel="Create"
        pending={createRole.isPending}
        canSubmit={Boolean(createForm.roleCode.trim() && createForm.roleName.trim())}
      >
        <div>
          <label htmlFor="role-code" className="mb-2 block text-sm font-medium text-ink">
            Role Code <span className="text-red-500">*</span>
          </label>
          <Input
            id="role-code"
            autoFocus
            value={createForm.roleCode}
            onChange={(event) => setCreateForm((f) => ({ ...f, roleCode: event.target.value.toUpperCase() }))}
            placeholder="e.g. OPS_ADMIN"
            className="font-mono text-xs"
          />
          <FieldError message={createFieldErrors.roleCode} />
        </div>
        <div>
          <label htmlFor="role-name" className="mb-2 block text-sm font-medium text-ink">
            Role Name <span className="text-red-500">*</span>
          </label>
          <Input
            id="role-name"
            value={createForm.roleName}
            onChange={(event) => setCreateForm((f) => ({ ...f, roleName: event.target.value }))}
            placeholder="e.g. Ops Admin"
          />
          <FieldError message={createFieldErrors.roleName} />
        </div>
        <div>
          <label htmlFor="role-type" className="mb-2 block text-sm font-medium text-ink">
            Type
          </label>
          <Select
            id="role-type"
            value={createForm.type}
            onChange={(event) => setCreateForm((f) => ({ ...f, type: event.target.value as RoleType }))}
          >
            {ROLE_TYPES.map((type) => (
              <option key={type} value={type}>
                {typeLabel[type]}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <label htmlFor="role-desc" className="mb-2 block text-sm font-medium text-ink">
            Description
          </label>
          <Textarea
            id="role-desc"
            rows={3}
            value={createForm.description}
            onChange={(event) => setCreateForm((f) => ({ ...f, description: event.target.value }))}
            placeholder="What can this role do?"
          />
        </div>
        <FormError message={createError ?? undefined} />
      </FormDialog>

      <FormDialog
        open={editOpen}
        title="Edit Role"
        onCancel={() => setEditOpen(false)}
        onSubmit={submitEdit}
        submitLabel="Save Changes"
        pending={updateRole.isPending}
        canSubmit={Boolean(editForm.roleName.trim())}
      >
        <div>
          <label htmlFor="edit-role-name" className="mb-2 block text-sm font-medium text-ink">
            Role Name <span className="text-red-500">*</span>
          </label>
          <Input
            id="edit-role-name"
            value={editForm.roleName}
            onChange={(event) => setEditForm((f) => ({ ...f, roleName: event.target.value }))}
          />
          <FieldError message={editFieldErrors.roleName} />
        </div>
        <div>
          <label htmlFor="edit-role-type" className="mb-2 block text-sm font-medium text-ink">
            Type
          </label>
          <Select
            id="edit-role-type"
            value={editForm.type}
            onChange={(event) => setEditForm((f) => ({ ...f, type: event.target.value as RoleType }))}
          >
            {ROLE_TYPES.map((type) => (
              <option key={type} value={type}>
                {typeLabel[type]}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <label htmlFor="edit-role-desc" className="mb-2 block text-sm font-medium text-ink">
            Description
          </label>
          <Textarea
            id="edit-role-desc"
            rows={3}
            value={editForm.description}
            onChange={(event) => setEditForm((f) => ({ ...f, description: event.target.value }))}
          />
        </div>
        <div>
          <span className="mb-2 block text-sm font-medium text-ink">Status</span>
          <Switch
            checked={editForm.isActive}
            onCheckedChange={(value) => setEditForm((f) => ({ ...f, isActive: value }))}
            label={editForm.isActive ? 'Active' : 'Inactive'}
          />
        </div>
        <FormError message={editError ?? undefined} />
      </FormDialog>

      <ConfirmDialog
        open={confirmOpen}
        title="Deactivate role"
        message={`Deactivate “${selected?.roleName ?? ''}”? Users keeping only this role may lose access.`}
        confirmLabel="Deactivate"
        pending={deactivateRole.isPending}
        onConfirm={submitDeactivate}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
