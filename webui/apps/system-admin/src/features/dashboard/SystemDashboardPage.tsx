import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, CardHeader, CardTitle, Badge, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, FileTextIcon, LockIcon, MoreVerticalIcon, ShieldCheckIcon, TrendingUpIcon, UsersIcon, PageHeader, StatCard, StatusBadge, Toolbar, TableFooter, TableState, NotBackedYet, EMPTY, formatCount, formatDate } from '@ui';
import { useDashboardStats, useUsers } from '../../api/queries';

const PREVIEW_SIZE = 5;

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || '?';
}

export function SystemDashboardPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('All Status');
  const [page, setPage] = useState(1);

  const stats = useDashboardStats();
  const users = useUsers({ page, size: PREVIEW_SIZE });

  const visible = (users.data?.content ?? [])
    .filter((user) => status === 'All Status' || (status === 'Active') === user.isActive)
    .filter(
      (user) =>
        user.fullName.toLowerCase().includes(search.toLowerCase()) ||
        user.email.toLowerCase().includes(search.toLowerCase())
    );

  const value = (n: number | undefined, suffix = '') =>
    stats.isLoading ? '…' : n === undefined ? EMPTY : `${formatCount(n)}${suffix}`;

  return (
    <div className="px-8 py-7">
      <PageHeader title="Dashboard" subtitle="Platform access and activity overview" />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <StatCard icon={<UsersIcon size={20} />} label="Active Users" tone="emerald" value={value(stats.data?.activeUsers)} />
        <StatCard icon={<ShieldCheckIcon size={20} />} label="Role Count" tone="sky" value={value(stats.data?.roleCount)} />
        <StatCard icon={<FileTextIcon size={20} />} label="Recent Audit Events" tone="indigo" value={value(stats.data?.recentAuditEvents)} />
        <StatCard icon={<LockIcon size={20} />} label="Failed Logins" tone="red" value={value(stats.data?.failedLogins)} />
        <StatCard
          icon={<TrendingUpIcon size={20} />}
          label="Endpoint Health"
          tone="teal"
          value={stats.isLoading ? '…' : stats.data ? `${stats.data.endpointHealthPercent}%` : EMPTY}
        />
      </div>

      <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,420px)]">
        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>User Directory</CardTitle>
            <Button size="sm" asChild>
              <Link to="/users">Add User</Link>
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
                <TableHead className="w-9 px-2" />
              </tr>
            </TableHeader>
            <TableBody>
              <TableState colSpan={5} isLoading={users.isLoading} isError={users.isError} isEmpty={visible.length === 0} entity="users" />
              {visible.map((user) => (
                <TableRow key={user.accountId}>
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
            from={visible.length === 0 ? 0 : (page - 1) * PREVIEW_SIZE + 1}
            to={(page - 1) * PREVIEW_SIZE + visible.length}
            total={users.data?.totalElements ?? 0}
            noun="users"
            page={page}
            totalPages={Math.max(users.data?.totalPages ?? 1, 1)}
            onPageChange={setPage}
          />
        </Card>

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Audit Timeline</CardTitle>
          </CardHeader>
          <NotBackedYet what="Audit event feed" />
        </Card>
      </div>
    </div>
  );
}
