import { useEffect, useState } from 'react';
import { Button, Card, CardHeader, CardTitle, Input, Textarea, Select, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, cn, MoreVerticalIcon, PencilIcon, PlusIcon, TrashIcon, PageHeader, Toolbar, StatusBadge, TableFooter, SlideOver, NotBackedYet, TableState, FieldError, FormError, ConfirmDialog, EMPTY, formatDate } from '@ui';
import type { ProductResponse } from '@domain/index';
import { useBrands, useCategories, useNameLookups, useProductOptions, useProducts } from '../../api/queries';
import { useCreateProduct, useDeactivateProduct, useUpdateProduct } from '../../api/mutations';
import { parseApiError } from '@api/apiError';

const PAGE_SIZE = 10;

type FormState = { productName: string; productDescription: string; categoryId: string; brandId: string };
const emptyForm: FormState = { productName: '', productDescription: '', categoryId: '', brandId: '' };

export function CommerceProductsPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('All Status');
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data, isLoading, isError } = useProducts({ page, size: PAGE_SIZE });
  const { categoryNames, brandNames } = useNameLookups();
  const { data: categoryPage } = useCategories({ page: 1, size: 200 });
  const { data: brandPage } = useBrands({ page: 1, size: 200 });
  const { data: options } = useProductOptions(selectedId);
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();
  const deactivateProduct = useDeactivateProduct();

  const products = data?.content ?? [];
  const selected = products.find((item) => item.productId === selectedId) ?? null;
  const isEdit = selected !== null;

  const visible = products
    .filter((product) => status === 'All Status' || (status === 'Active') === product.isActive)
    .filter((product) => product.productName.toLowerCase().includes(search.toLowerCase()));

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

  const openEdit = (product: ProductResponse) => {
    resetErrors();
    setSelectedId(product.productId);
    setForm({
      productName: product.productName,
      productDescription: product.productDescription ?? '',
      categoryId: product.categoryId,
      brandId: product.brandId
    });
    setPanelOpen(true);
  };

  // Keep the form in sync if the selected product refetches after an edit.
  useEffect(() => {
    if (selected) {
      setForm({
        productName: selected.productName,
        productDescription: selected.productDescription ?? '',
        categoryId: selected.categoryId,
        brandId: selected.brandId
      });
    }
  }, [selected]);

  const setField = (patch: Partial<FormState>) => setForm((current) => ({ ...current, ...patch }));

  const handleError = (error: unknown) => {
    const parsed = parseApiError(error);
    setFormError(parsed.message);
    setFieldErrors(parsed.fieldErrors);
  };

  const submit = () => {
    resetErrors();
    if (isEdit && selected) {
      updateProduct.mutate(
        { productId: selected.productId, productName: form.productName.trim(), productDescription: form.productDescription.trim() },
        { onError: handleError }
      );
    } else {
      createProduct.mutate(
        {
          categoryId: form.categoryId,
          brandId: form.brandId,
          productName: form.productName.trim(),
          productDescription: form.productDescription.trim() || undefined
        },
        { onSuccess: () => setPanelOpen(false), onError: handleError }
      );
    }
  };

  const submitDeactivate = () => {
    if (!selected) return;
    deactivateProduct.mutate(selected.productId, {
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

  const saving = createProduct.isPending || updateProduct.isPending;
  const canSubmit = isEdit
    ? Boolean(form.productName.trim())
    : Boolean(form.productName.trim() && form.categoryId && form.brandId);

  return (
    <div className="flex h-full overflow-hidden">
      <div className="scroll-slim min-w-0 flex-1 overflow-y-auto px-8 py-7">
        <PageHeader
          title="Products"
          subtitle="Manage your product catalog"
          actions={
            <>
              <Button size="lg" onClick={openCreate}>
                New Product
              </Button>
              <Button variant="outline" size="icon" aria-label="More product actions" className="h-11 w-11">
                <MoreVerticalIcon size={17} />
              </Button>
            </>
          }
        />

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Product Catalog</CardTitle>
          </CardHeader>
          <div className="px-5 py-4">
            <Toolbar
              search={search}
              onSearchChange={setSearch}
              searchPlaceholder="Search products..."
              filters={[
                { id: 'status', value: status, onChange: setStatus, options: ['All Status', 'Active', 'Inactive'] }
              ]}
            />
          </div>
          <Table>
            <TableHeader>
              <tr>
                <TableHead>Product ID</TableHead>
                <TableHead>Product Name</TableHead>
                <TableHead>Category</TableHead>
                <TableHead>Brand</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Created At</TableHead>
                <TableHead className="w-9 px-2" />
              </tr>
            </TableHeader>
            <TableBody>
              <TableState colSpan={7} isLoading={isLoading} isError={isError} isEmpty={visible.length === 0} entity="products" />
              {visible.map((product) => (
                <TableRow
                  key={product.productId}
                  selected={selectedId === product.productId}
                  onClick={() => openEdit(product)}
                  className="cursor-pointer"
                >
                  <TableCell className="whitespace-nowrap font-mono text-[11px]">{product.productId}</TableCell>
                  <TableCell className="text-ink">{product.productName}</TableCell>
                  <TableCell className="whitespace-nowrap">{categoryNames.get(product.categoryId) ?? EMPTY}</TableCell>
                  <TableCell>{brandNames.get(product.brandId) ?? EMPTY}</TableCell>
                  <TableCell>
                    <StatusBadge isActive={product.isActive} />
                  </TableCell>
                  <TableCell>{formatDate(product.createdAt)}</TableCell>
                  <TableCell className="px-2">
                    <Button variant="ghost" size="icon" className="h-8 w-8" aria-label={`Actions for ${product.productName}`}>
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
            noun="products"
            page={page}
            totalPages={Math.max(data?.totalPages ?? 1, 1)}
            onPageChange={setPage}
          />
        </Card>
      </div>

      {panelOpen ? (
        <SlideOver
          title={isEdit ? 'Edit Product' : 'New Product'}
          subtitle={isEdit ? 'Update this product' : 'Create a new product'}
          onClose={() => setPanelOpen(false)}
          className="w-[480px] shrink-0"
          footer={
            <div className="flex items-center gap-3">
              {isEdit ? (
                <Button
                  variant="outline"
                  className="flex-1"
                  disabled={!selected?.isActive || deactivateProduct.isPending}
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
            <p className="text-sm font-semibold text-ink">Product Details</p>

            <div>
              <label htmlFor="product-name" className="mb-2 block text-sm text-ink">
                Product Name <span className="text-red-500">*</span>
              </label>
              <Input
                id="product-name"
                required
                value={form.productName}
                onChange={(event) => setField({ productName: event.target.value })}
                placeholder="Enter product name"
              />
              <FieldError message={fieldErrors.productName} />
            </div>

            <div>
              <label htmlFor="product-description" className="mb-2 block text-sm text-ink">
                Description
              </label>
              <Textarea
                id="product-description"
                rows={4}
                value={form.productDescription}
                onChange={(event) => setField({ productDescription: event.target.value })}
                placeholder="Enter product description"
              />
              <FieldError message={fieldErrors.productDescription} />
            </div>

            <div>
              <label htmlFor="product-category" className="mb-2 block text-sm text-ink">
                Category <span className="text-red-500">*</span>
              </label>
              <Select
                id="product-category"
                required
                value={form.categoryId}
                disabled={isEdit}
                onChange={(event) => setField({ categoryId: event.target.value })}
              >
                <option value="" disabled>
                  Select category
                </option>
                {(categoryPage?.content ?? []).map((category) => (
                  <option key={category.categoryId} value={category.categoryId}>
                    {category.categoryName}
                  </option>
                ))}
              </Select>
              <FieldError message={fieldErrors.categoryId} />
            </div>

            <div>
              <label htmlFor="product-brand" className="mb-2 block text-sm text-ink">
                Brand <span className="text-red-500">*</span>
              </label>
              <Select
                id="product-brand"
                required
                value={form.brandId}
                disabled={isEdit}
                onChange={(event) => setField({ brandId: event.target.value })}
              >
                <option value="" disabled>
                  Select brand
                </option>
                {(brandPage?.content ?? []).map((brand) => (
                  <option key={brand.brandId} value={brand.brandId}>
                    {brand.brandName}
                  </option>
                ))}
              </Select>
              <FieldError message={fieldErrors.brandId} />
            </div>

            {isEdit ? (
              <p className="text-xs text-ink-subtle">Category and brand are fixed after creation.</p>
            ) : null}

            <FormError message={formError ?? undefined} />

            <div>
              <p className="mb-2 text-sm font-semibold text-ink">Product Images</p>
              <NotBackedYet what="Image upload" />
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between">
                <p className="text-sm font-semibold text-ink">Options</p>
                <Button type="button" variant="outline" size="sm" disabled={!isEdit}>
                  <PlusIcon size={14} />
                  Add Option
                </Button>
              </div>
              {!isEdit ? (
                <p className="rounded-lg border border-dashed border-line px-3 py-4 text-center text-xs text-ink-subtle">
                  Save the product first, then add options.
                </p>
              ) : (
                <div className="overflow-hidden rounded-lg border border-line">
                  <Table>
                    <TableHeader>
                      <tr>
                        <TableHead>Option Name</TableHead>
                        <TableHead>Option Values</TableHead>
                        <TableHead>Actions</TableHead>
                      </tr>
                    </TableHeader>
                    <TableBody>
                      {(options ?? []).length === 0 ? (
                        <TableRow className="hover:bg-transparent">
                          <TableCell colSpan={3} className="py-6 text-center text-sm text-ink-subtle">
                            No options yet.
                          </TableCell>
                        </TableRow>
                      ) : (
                        (options ?? []).map((option) => (
                          <TableRow key={option.optionId}>
                            <TableCell className="text-ink">{option.optionName}</TableCell>
                            <TableCell className="text-xs">
                              {option.values.map((value) => value.value).join(', ') || EMPTY}
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-1.5">
                                <Button variant="outline" size="icon" aria-label={`Edit ${option.optionName}`} className="h-8 w-8">
                                  <PencilIcon size={14} />
                                </Button>
                                <Button variant="danger" size="icon" aria-label={`Delete ${option.optionName}`} className="h-8 w-8">
                                  <TrashIcon size={14} />
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          </form>
        </SlideOver>
      ) : null}

      <ConfirmDialog
        open={confirmOpen}
        title="Deactivate product"
        message={`Deactivate “${selected?.productName ?? ''}”? It will be marked inactive.`}
        confirmLabel="Deactivate"
        pending={deactivateProduct.isPending}
        onConfirm={submitDeactivate}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
