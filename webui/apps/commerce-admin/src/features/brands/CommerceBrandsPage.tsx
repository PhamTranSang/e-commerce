import { useState } from 'react';
import { Button, Card, CardHeader, CardTitle, Input, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, ClockIcon, MoreVerticalIcon, PencilIcon, XCircleIcon, PageHeader, Toolbar, StatusBadge, TableFooter, SlideOver, NotBackedYet, TableState, FieldError, FormError, ConfirmDialog, EMPTY, formatDate, formatDateTime } from '@ui';
import type { BrandResponse } from '@domain/index';
import { useBrands } from '../../api/queries';
import { useCreateBrand, useDeactivateBrand, useRenameBrand } from '../../api/mutations';
import { parseApiError } from '@api/apiError';

const PAGE_SIZE = 10;
type PanelMode = 'view' | 'create';

export function CommerceBrandsPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('All Status');
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<BrandResponse | null>(null);
  const [mode, setMode] = useState<PanelMode>('view');
  const [renaming, setRenaming] = useState(false);
  const [nameDraft, setNameDraft] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data, isLoading, isError } = useBrands({ page, size: PAGE_SIZE });
  const createBrand = useCreateBrand();
  const renameBrand = useRenameBrand();
  const deactivateBrand = useDeactivateBrand();

  const brands = data?.content ?? [];
  const visible = brands
    .filter((brand) => status === 'All Status' || (status === 'Active') === brand.isActive)
    .filter((brand) => brand.brandName.toLowerCase().includes(search.toLowerCase()));

  const resetErrors = () => {
    setFormError(null);
    setFieldError(undefined);
  };

  const openCreate = () => {
    resetErrors();
    setMode('create');
    setSelected(null);
    setRenaming(false);
    setNameDraft('');
  };

  const openBrand = (brand: BrandResponse) => {
    resetErrors();
    setMode('view');
    setSelected(brand);
    setRenaming(false);
    setNameDraft(brand.brandName);
  };

  const closePanel = () => {
    setSelected(null);
    setMode('view');
    setRenaming(false);
    resetErrors();
  };

  const submitCreate = () => {
    resetErrors();
    createBrand.mutate(
      { brandName: nameDraft.trim() },
      {
        onSuccess: () => closePanel(),
        onError: (error) => {
          const parsed = parseApiError(error);
          setFormError(parsed.message);
          setFieldError(parsed.fieldErrors.brandName);
        }
      }
    );
  };

  const submitRename = () => {
    if (!selected) return;
    resetErrors();
    renameBrand.mutate(
      { brandId: selected.brandId, brandName: nameDraft.trim() },
      {
        onSuccess: (updated) => {
          setSelected(updated);
          setRenaming(false);
        },
        onError: (error) => {
          const parsed = parseApiError(error);
          setFormError(parsed.message);
          setFieldError(parsed.fieldErrors.brandName);
        }
      }
    );
  };

  const submitDeactivate = () => {
    if (!selected) return;
    deactivateBrand.mutate(selected.brandId, {
      onSuccess: () => {
        setConfirmOpen(false);
        closePanel();
      },
      onError: (error) => {
        setConfirmOpen(false);
        setFormError(parseApiError(error).message);
      }
    });
  };

  const detailRows: [string, React.ReactNode][] = selected
    ? [
        ['Brand Name', selected.brandName],
        ['Status', <StatusBadge key="status" isActive={selected.isActive} />],
        ['Product Count', EMPTY],
        ['Created At', formatDateTime(selected.createdAt)],
        ['Updated At', formatDateTime(selected.updatedAt)],
        ['Created By', EMPTY],
        ['Updated By', EMPTY]
      ]
    : [];

  const panelOpen = mode === 'create' || selected !== null;

  return (
    <div className="flex h-full overflow-hidden">
      <div className="scroll-slim min-w-0 flex-1 overflow-y-auto px-8 py-7">
        <PageHeader
          title="Brands"
          subtitle="Manage and organize all product brands"
          actions={
            <Button size="lg" onClick={openCreate}>
              Create Brand
            </Button>
          }
        />

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Brand Catalog</CardTitle>
            <Button variant="ghost" size="icon" aria-label="Brand catalog options">
              <MoreVerticalIcon size={16} />
            </Button>
          </CardHeader>
          <div className="px-5 py-4">
            <Toolbar
              search={search}
              onSearchChange={setSearch}
              searchPlaceholder="Search brands..."
              filters={[
                { id: 'status', value: status, onChange: setStatus, options: ['All Status', 'Active', 'Inactive'] }
              ]}
            />
          </div>
          <Table>
            <TableHeader>
              <tr>
                <TableHead>Brand Name</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Product Count</TableHead>
                <TableHead>Created At</TableHead>
                <TableHead>Updated At</TableHead>
                <TableHead>Actions</TableHead>
              </tr>
            </TableHeader>
            <TableBody>
              <TableState colSpan={6} isLoading={isLoading} isError={isError} isEmpty={visible.length === 0} entity="brands" />
              {visible.map((brand) => (
                <TableRow
                  key={brand.brandId}
                  selected={selected?.brandId === brand.brandId}
                  onClick={() => openBrand(brand)}
                  className="cursor-pointer"
                >
                  <TableCell className="text-ink">{brand.brandName}</TableCell>
                  <TableCell>
                    <StatusBadge isActive={brand.isActive} />
                  </TableCell>
                  <TableCell className="text-ink-subtle">{EMPTY}</TableCell>
                  <TableCell>{formatDate(brand.createdAt)}</TableCell>
                  <TableCell>{formatDate(brand.updatedAt)}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1.5">
                      <Button
                        variant="outline"
                        size="icon"
                        aria-label={`Edit ${brand.brandName}`}
                        className="h-8 w-8"
                        onClick={(event) => {
                          event.stopPropagation();
                          openBrand(brand);
                          setRenaming(true);
                        }}
                      >
                        <PencilIcon size={15} />
                      </Button>
                      <Button
                        variant="outline"
                        size="icon"
                        aria-label={`More actions for ${brand.brandName}`}
                        className="h-8 w-8"
                      >
                        <MoreVerticalIcon size={15} />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TableFooter
            from={visible.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1}
            to={(page - 1) * PAGE_SIZE + visible.length}
            total={data?.totalElements ?? 0}
            noun="brands"
            page={page}
            totalPages={Math.max(data?.totalPages ?? 1, 1)}
            onPageChange={setPage}
          />
        </Card>

        <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,480px)]">
          <Card>
            <CardHeader>
              <CardTitle>Brand Gallery</CardTitle>
            </CardHeader>
            <NotBackedYet what="Brand logo artwork" />
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Brand Summary</CardTitle>
            </CardHeader>
            <NotBackedYet what="Active/inactive brand breakdown" />
          </Card>
        </div>
      </div>

      {panelOpen ? (
        <SlideOver
          title={mode === 'create' ? 'Create Brand' : 'Selected Brand'}
          subtitle={mode === 'create' ? 'Add a new brand' : undefined}
          onClose={closePanel}
          className="w-[400px] shrink-0"
          footer={
            mode === 'create' ? (
              <div className="flex items-center gap-3">
                <Button variant="outline" className="flex-1" onClick={closePanel}>
                  Cancel
                </Button>
                <Button className="flex-1" onClick={submitCreate} disabled={!nameDraft.trim() || createBrand.isPending}>
                  {createBrand.isPending ? 'Saving…' : 'Create'}
                </Button>
              </div>
            ) : undefined
          }
        >
          {mode === 'create' ? (
            <div className="space-y-4">
              <div>
                <label htmlFor="brand-name" className="mb-2 block text-sm font-medium text-ink">
                  Brand Name <span className="text-red-500">*</span>
                </label>
                <Input
                  id="brand-name"
                  autoFocus
                  value={nameDraft}
                  onChange={(event) => setNameDraft(event.target.value)}
                  placeholder="Enter brand name"
                />
                <FieldError message={fieldError} />
              </div>
              <FormError message={formError ?? undefined} />
            </div>
          ) : selected ? (
            <>
              {renaming ? (
                <div className="space-y-3">
                  <label htmlFor="rename-brand" className="block text-sm font-medium text-ink">
                    Brand Name
                  </label>
                  <Input
                    id="rename-brand"
                    autoFocus
                    value={nameDraft}
                    onChange={(event) => setNameDraft(event.target.value)}
                  />
                  <FieldError message={fieldError} />
                  <div className="flex gap-2">
                    <Button size="sm" onClick={submitRename} disabled={!nameDraft.trim() || renameBrand.isPending}>
                      {renameBrand.isPending ? 'Saving…' : 'Save'}
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setRenaming(false);
                        setNameDraft(selected.brandName);
                        resetErrors();
                      }}
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              ) : (
                <p className="text-xl font-bold text-ink">{selected.brandName}</p>
              )}
              <StatusBadge isActive={selected.isActive} className="mt-2" />

              <FormError message={formError ?? undefined} />

              <dl className="mt-6 space-y-3.5 border-t border-line pt-5">
                {detailRows.map(([label, value]) => (
                  <div key={label} className="flex items-start justify-between gap-4">
                    <dt className="text-sm font-medium text-ink">{label}</dt>
                    <dd className="text-right text-sm text-ink-muted">{value}</dd>
                  </div>
                ))}
              </dl>

              <div className="mt-6 grid grid-cols-2 gap-3">
                <Button variant="outline" onClick={() => setRenaming(true)} disabled={renaming}>
                  <PencilIcon size={16} className="text-brand" />
                  Rename
                </Button>
                <Button
                  variant="danger"
                  onClick={() => setConfirmOpen(true)}
                  disabled={!selected.isActive || deactivateBrand.isPending}
                >
                  <XCircleIcon size={16} />
                  Deactivate
                </Button>
              </div>

              <div className="mt-7 border-t border-line pt-5">
                <p className="mb-1 flex items-center gap-2 text-sm font-semibold text-ink">
                  <ClockIcon size={16} className="text-ink-subtle" />
                  Recent Updates
                </p>
                <NotBackedYet what="Brand audit history" />
              </div>
            </>
          ) : null}
        </SlideOver>
      ) : null}

      <ConfirmDialog
        open={confirmOpen}
        title="Deactivate brand"
        message={`Deactivate “${selected?.brandName ?? ''}”? It will be marked inactive.`}
        confirmLabel="Deactivate"
        pending={deactivateBrand.isPending}
        onConfirm={submitDeactivate}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
