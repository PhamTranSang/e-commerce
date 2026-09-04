import { useEffect, useState } from 'react';
import { Button, Card, CardHeader, CardTitle, IconInput, Input, Select, FilterIcon, MoreVerticalIcon, PencilIcon, PlusCircleIcon, SearchIcon, XCircleIcon, PageHeader, StatusBadge, NotBackedYet, FieldError, FormError, ConfirmDialog, FormDialog, EMPTY, formatDateTime } from '@ui';
import type { CategoryResponse, CategoryTreeResponse } from '@domain/index';
import { CategoryTree, TreeLegend } from '../../components/CategoryTree';
import { useCategories, useCategoryTree } from '../../api/queries';
import { useCreateCategory, useDeactivateCategory, useRenameCategory } from '../../api/mutations';
import { parseApiError } from '@api/apiError';

export function CommerceCategoriesPage() {
  const [treeFilter, setTreeFilter] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [nameError, setNameError] = useState<string | undefined>();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createParent, setCreateParent] = useState('');
  const [createError, setCreateError] = useState<string | null>(null);
  const [createNameError, setCreateNameError] = useState<string | undefined>();

  const { data: tree, isLoading: treeLoading, isError: treeError } = useCategoryTree();
  const { data: page } = useCategories({ page: 1, size: 200 });
  const createCategory = useCreateCategory();
  const renameCategory = useRenameCategory();
  const deactivateCategory = useDeactivateCategory();

  const flat = page?.content ?? [];
  const detail = flat.find((category) => category.categoryId === selectedId) ?? null;

  useEffect(() => {
    if (detail) setName(detail.categoryName);
  }, [detail]);

  const selectCategory = (node: CategoryTreeResponse) => {
    setSelectedId(node.categoryId);
    setName(node.categoryName);
    setFormError(null);
    setNameError(undefined);
  };

  const parentName = (category: CategoryResponse | null) =>
    category?.parentId
      ? (flat.find((c) => c.categoryId === category.parentId)?.categoryName ?? category.parentId)
      : EMPTY;

  const submitRename = () => {
    if (!detail) return;
    setFormError(null);
    setNameError(undefined);
    renameCategory.mutate(
      { categoryId: detail.categoryId, categoryName: name.trim() },
      {
        onError: (error) => {
          const parsed = parseApiError(error);
          setFormError(parsed.message);
          setNameError(parsed.fieldErrors.categoryName);
        }
      }
    );
  };

  const submitDeactivate = () => {
    if (!detail) return;
    deactivateCategory.mutate(detail.categoryId, {
      onSuccess: () => {
        setConfirmOpen(false);
        setSelectedId(null);
      },
      onError: (error) => {
        setConfirmOpen(false);
        setFormError(parseApiError(error).message);
      }
    });
  };

  const submitCreate = () => {
    setCreateError(null);
    setCreateNameError(undefined);
    createCategory.mutate(
      { categoryName: createName.trim(), parentId: createParent || null },
      {
        onSuccess: () => {
          setCreateOpen(false);
          setCreateName('');
          setCreateParent('');
        },
        onError: (error) => {
          const parsed = parseApiError(error);
          setCreateError(parsed.message);
          setCreateNameError(parsed.fieldErrors.categoryName);
        }
      }
    );
  };

  const detailRows: [string, React.ReactNode][] = [
    ['Category ID', detail?.categoryId ?? EMPTY],
    ['Parent', detail ? parentName(detail) : EMPTY],
    ['Status', detail ? <StatusBadge key="status" isActive={detail.isActive} /> : EMPTY],
    ['Child Categories', EMPTY],
    ['Products', EMPTY],
    ['Created At', formatDateTime(detail?.createdAt)],
    ['Created By', EMPTY],
    ['Updated At', formatDateTime(detail?.updatedAt)],
    ['Updated By', EMPTY]
  ];

  return (
    <div className="px-8 py-7">
      <PageHeader
        breadcrumbs={['Dashboard', 'Categories']}
        title="Categories"
        subtitle="Manage your catalog categories and organization."
        actions={
          <>
            <Button size="lg" onClick={() => setCreateOpen(true)}>
              <PlusCircleIcon size={17} />
              Create Category
            </Button>
            <Button variant="outline" size="lg" disabled={!detail} onClick={submitRename}>
              <PencilIcon size={17} className="text-ink-muted" />
              Rename
            </Button>
            <Button
              variant="warning"
              size="lg"
              disabled={!detail || !detail.isActive}
              onClick={() => setConfirmOpen(true)}
            >
              <XCircleIcon size={17} />
              Deactivate
            </Button>
            <Button variant="outline" size="icon" aria-label="More category actions" className="h-11 w-11">
              <MoreVerticalIcon size={17} />
            </Button>
          </>
        }
      />

      <div className="grid gap-5 xl:grid-cols-[minmax(0,360px)_minmax(0,1fr)_minmax(0,400px)]">
        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Category Tree</CardTitle>
          </CardHeader>
          <div className="flex items-center gap-2 px-5 py-4">
            <IconInput
              value={treeFilter}
              onChange={(event) => setTreeFilter(event.target.value)}
              placeholder="Search categories..."
              aria-label="Search categories"
              trailing={<SearchIcon size={16} className="text-ink-subtle" />}
            />
            <Button variant="outline" size="icon" aria-label="Filter categories">
              <FilterIcon size={16} />
            </Button>
          </div>
          <div className="scroll-slim min-h-[420px] flex-1 overflow-y-auto px-3 pb-4">
            {treeLoading ? (
              <p className="px-2 py-6 text-sm text-ink-subtle">Loading categories…</p>
            ) : treeError ? (
              <p className="px-2 py-6 text-sm text-ink-subtle">Could not load categories.</p>
            ) : (tree?.length ?? 0) === 0 ? (
              <p className="px-2 py-6 text-sm text-ink-subtle">No categories yet.</p>
            ) : (
              <CategoryTree
                nodes={tree ?? []}
                selectedId={selectedId ?? undefined}
                onSelect={selectCategory}
                filter={treeFilter}
              />
            )}
          </div>
          <TreeLegend total={page?.totalElements} className="border-t border-line px-5 py-3.5" />
        </Card>

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Selected Category</CardTitle>
            <Button variant="ghost" size="icon" aria-label="Selected category options">
              <MoreVerticalIcon size={16} />
            </Button>
          </CardHeader>
          {detail ? (
            <form
              className="flex flex-1 flex-col gap-5 p-5"
              onSubmit={(event) => {
                event.preventDefault();
                submitRename();
              }}
            >
              <div>
                <label htmlFor="category-name" className="mb-2 block text-sm font-medium text-ink">
                  Category Name <span className="text-red-500">*</span>
                </label>
                <Input id="category-name" required value={name} onChange={(event) => setName(event.target.value)} />
                <FieldError message={nameError} />
              </div>

              <div>
                <span className="mb-2 block text-sm font-medium text-ink">Parent</span>
                <Input value={parentName(detail)} readOnly className="bg-surface-muted" />
              </div>

              <div>
                <span className="mb-2 block text-sm font-medium text-ink">Status</span>
                <StatusBadge isActive={detail.isActive} />
                <p className="mt-1.5 text-xs text-ink-subtle">
                  Use Deactivate to take a category offline. Slug, description and display order are not part of the
                  category API yet.
                </p>
              </div>

              <FormError message={formError ?? undefined} />

              <div className="mt-auto flex items-center gap-3 pt-2">
                <Button type="submit" disabled={!name.trim() || renameCategory.isPending}>
                  {renameCategory.isPending ? 'Saving…' : 'Save Changes'}
                </Button>
                <Button type="button" variant="outline" onClick={() => setName(detail.categoryName)}>
                  Cancel
                </Button>
              </div>
            </form>
          ) : (
            <p className="p-5 text-sm text-ink-subtle">Select a category in the tree to edit it.</p>
          )}
        </Card>

        <div className="space-y-5">
          <Card>
            <CardHeader>
              <CardTitle>Category Details</CardTitle>
            </CardHeader>
            <dl className="space-y-3.5 p-5">
              {detailRows.map(([label, value]) => (
                <div key={label} className="flex items-start justify-between gap-4">
                  <dt className="text-sm text-ink-muted">{label}</dt>
                  <dd className="text-right text-sm text-ink">{value}</dd>
                </div>
              ))}
            </dl>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Recent Changes</CardTitle>
            </CardHeader>
            <NotBackedYet what="Category audit history" />
          </Card>
        </div>
      </div>

      <FormDialog
        open={createOpen}
        title="Create Category"
        onCancel={() => setCreateOpen(false)}
        onSubmit={submitCreate}
        submitLabel="Create"
        pending={createCategory.isPending}
        canSubmit={Boolean(createName.trim())}
      >
        <div>
          <label htmlFor="new-category-name" className="mb-2 block text-sm font-medium text-ink">
            Category Name <span className="text-red-500">*</span>
          </label>
          <Input
            id="new-category-name"
            autoFocus
            value={createName}
            onChange={(event) => setCreateName(event.target.value)}
            placeholder="Enter category name"
          />
          <FieldError message={createNameError} />
        </div>
        <div>
          <label htmlFor="new-category-parent" className="mb-2 block text-sm font-medium text-ink">
            Parent
          </label>
          <Select
            id="new-category-parent"
            value={createParent}
            onChange={(event) => setCreateParent(event.target.value)}
          >
            <option value="">None (top level)</option>
            {flat.map((category) => (
              <option key={category.categoryId} value={category.categoryId}>
                {category.categoryName}
              </option>
            ))}
          </Select>
        </div>
        <FormError message={createError ?? undefined} />
      </FormDialog>

      <ConfirmDialog
        open={confirmOpen}
        title="Deactivate category"
        message={`Deactivate “${detail?.categoryName ?? ''}”? It will be marked inactive.`}
        confirmLabel="Deactivate"
        pending={deactivateCategory.isPending}
        onConfirm={submitDeactivate}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
