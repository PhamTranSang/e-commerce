import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Input, Select, Checkbox, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, MoreVerticalIcon, PageHeader, Toolbar, StatusBadge, TableFooter, SlideOver, TableState, FieldError, FormError, ConfirmDialog, EMPTY, formatAmount } from '@ui';
import type { SkuResponse } from '@domain/index';
import { useProductOptions, useProducts, useSkus } from '../../api/queries';
import { useCreateSku, useDeactivateSku, useUpdateSku } from '../../api/mutations';
import { parseApiError } from '@api/apiError';

const PAGE_SIZE = 10;

export function CommerceSkusPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('All Status');
  const [page, setPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [panelOpen, setPanelOpen] = useState(false);
  const [editing, setEditing] = useState<SkuResponse | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  // Create/edit form fields.
  const [productId, setProductId] = useState('');
  const [skuCode, setSkuCode] = useState('');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [weightGrams, setWeightGrams] = useState('');
  const [optionChoices, setOptionChoices] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const { data, isLoading, isError } = useSkus({ page, size: PAGE_SIZE });
  const { data: productPage } = useProducts({ page: 1, size: 200 });
  const { data: productOptions } = useProductOptions(editing ? null : productId || null);
  const createSku = useCreateSku();
  const updateSku = useUpdateSku();
  const deactivateSku = useDeactivateSku();

  const productNames = useMemo(
    () => new Map((productPage?.content ?? []).map((p) => [p.productId, p.productName])),
    [productPage]
  );

  const skus = data?.content ?? [];
  const visible = skus
    .filter((sku) => status === 'All Status' || (status === 'Active') === sku.isActive)
    .filter((sku) => sku.skuCode.toLowerCase().includes(search.toLowerCase()));

  const isEdit = editing !== null;
  const allSelected = visible.length > 0 && selectedIds.length === visible.length;
  const toggleAll = () => setSelectedIds(allSelected ? [] : visible.map((sku) => sku.skuId));
  const toggleOne = (id: string) =>
    setSelectedIds((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));

  const resetForm = () => {
    setProductId('');
    setSkuCode('');
    setAmount('');
    setCurrency('USD');
    setWeightGrams('');
    setOptionChoices({});
    setFormError(null);
    setFieldErrors({});
  };

  const openCreate = () => {
    setEditing(null);
    resetForm();
    setPanelOpen(true);
  };

  const openEdit = (sku: SkuResponse) => {
    setEditing(sku);
    setProductId(sku.productId);
    setSkuCode(sku.skuCode);
    setAmount(sku.amount);
    setCurrency(sku.currency);
    setWeightGrams(String(sku.weightGrams));
    setOptionChoices({});
    setFormError(null);
    setFieldErrors({});
    setPanelOpen(true);
  };

  // Reset option picks whenever the chosen product changes during creation.
  useEffect(() => {
    if (!isEdit) setOptionChoices({});
  }, [productId, isEdit]);

  const handleError = (error: unknown) => {
    const parsed = parseApiError(error);
    setFormError(parsed.message);
    setFieldErrors(parsed.fieldErrors);
  };

  const submit = () => {
    setFormError(null);
    setFieldErrors({});
    if (isEdit && editing) {
      updateSku.mutate(
        { skuId: editing.skuId, amount: amount.trim(), currency: currency.trim(), weightGrams: Number(weightGrams) },
        { onSuccess: () => setPanelOpen(false), onError: handleError }
      );
    } else {
      createSku.mutate(
        {
          productId,
          skuCode: skuCode.trim(),
          amount: amount.trim(),
          currency: currency.trim(),
          weightGrams: Number(weightGrams),
          optionValueIds: (productOptions ?? []).map((option) => optionChoices[option.optionId]).filter(Boolean)
        },
        { onSuccess: () => setPanelOpen(false), onError: handleError }
      );
    }
  };

  const submitDeactivate = () => {
    if (!editing) return;
    deactivateSku.mutate(editing.skuId, {
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

  // Every option of the chosen product must have a value selected.
  const optionsComplete = (productOptions ?? []).every((option) => optionChoices[option.optionId]);
  const saving = createSku.isPending || updateSku.isPending;
  const canSubmit = isEdit
    ? Boolean(amount.trim() && currency.trim() && weightGrams)
    : Boolean(productId && skuCode.trim() && amount.trim() && currency.trim() && weightGrams && optionsComplete);

  return (
    <div className="flex h-full overflow-hidden">
      <div className="scroll-slim min-w-0 flex-1 overflow-y-auto px-8 py-7">
        <PageHeader
          title="SKUs"
          subtitle="Inventory by SKU"
          actions={
            <Button size="lg" onClick={openCreate}>
              Add SKU
            </Button>
          }
        />

        <Card className="flex flex-col">
          <div className="px-5 py-4">
            <Toolbar
              search={search}
              onSearchChange={setSearch}
              searchPlaceholder="Search SKUs..."
              filters={[
                { id: 'status', value: status, onChange: setStatus, options: ['All Status', 'Active', 'Inactive'] }
              ]}
            />
          </div>
          <Table>
            <TableHeader>
              <tr>
                <TableHead className="w-10">
                  <Checkbox checked={allSelected} onChange={toggleAll} aria-label="Select all SKUs" />
                </TableHead>
                <TableHead>SKU Code</TableHead>
                <TableHead>Product</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Currency</TableHead>
                <TableHead>Weight (g)</TableHead>
                <TableHead>Stock</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-9 px-2" />
              </tr>
            </TableHeader>
            <TableBody>
              <TableState colSpan={9} isLoading={isLoading} isError={isError} isEmpty={visible.length === 0} entity="SKUs" />
              {visible.map((sku) => (
                <TableRow
                  key={sku.skuId}
                  selected={selectedIds.includes(sku.skuId) || editing?.skuId === sku.skuId}
                  onClick={() => openEdit(sku)}
                  className="cursor-pointer"
                >
                  <TableCell onClick={(event) => event.stopPropagation()}>
                    <Checkbox
                      checked={selectedIds.includes(sku.skuId)}
                      onChange={() => toggleOne(sku.skuId)}
                      aria-label={`Select ${sku.skuCode}`}
                    />
                  </TableCell>
                  <TableCell className="whitespace-nowrap font-mono text-[11px]">{sku.skuCode}</TableCell>
                  <TableCell className="text-ink">{productNames.get(sku.productId) ?? EMPTY}</TableCell>
                  <TableCell>{formatAmount(sku.amount)}</TableCell>
                  <TableCell>{sku.currency}</TableCell>
                  <TableCell>{sku.weightGrams}</TableCell>
                  <TableCell className="text-ink-subtle">{EMPTY}</TableCell>
                  <TableCell>
                    <StatusBadge isActive={sku.isActive} />
                  </TableCell>
                  <TableCell className="px-2">
                    <Button variant="ghost" size="icon" className="h-8 w-8" aria-label={`Actions for ${sku.skuCode}`}>
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
            noun="SKUs"
            page={page}
            totalPages={Math.max(data?.totalPages ?? 1, 1)}
            onPageChange={setPage}
          />
        </Card>
        <p className="mt-3 text-xs text-ink-subtle">
          Stock levels and warehouses are not part of the SKU API yet, so those columns stay empty.
        </p>
      </div>

      {panelOpen ? (
        <SlideOver
          title={isEdit ? 'Edit SKU' : 'Add SKU'}
          subtitle={isEdit ? 'Update pricing and weight' : 'Create a new SKU'}
          onClose={() => setPanelOpen(false)}
          className="w-[360px] shrink-0"
          footer={
            <div className="flex items-center gap-3">
              {isEdit ? (
                <Button
                  variant="outline"
                  className="flex-1"
                  disabled={!editing?.isActive || deactivateSku.isPending}
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
              <label htmlFor="sku-product" className="mb-2 block text-sm text-ink">
                Product <span className="text-red-500">*</span>
              </label>
              <Select
                id="sku-product"
                value={productId}
                disabled={isEdit}
                onChange={(event) => setProductId(event.target.value)}
              >
                <option value="" disabled>
                  Select product
                </option>
                {(productPage?.content ?? []).map((product) => (
                  <option key={product.productId} value={product.productId}>
                    {product.productName}
                  </option>
                ))}
              </Select>
              <FieldError message={fieldErrors.productId} />
            </div>

            <div>
              <label htmlFor="sku-code" className="mb-2 block text-sm text-ink">
                SKU Code <span className="text-red-500">*</span>
              </label>
              <Input
                id="sku-code"
                value={skuCode}
                disabled={isEdit}
                onChange={(event) => setSkuCode(event.target.value)}
                placeholder="e.g. IPH15P-256-NT"
                className="font-mono text-xs"
              />
              <FieldError message={fieldErrors.skuCode} />
            </div>

            {!isEdit && (productOptions ?? []).length > 0 ? (
              <div className="space-y-3">
                <p className="text-sm font-medium text-ink">Option Values</p>
                {(productOptions ?? []).map((option) => (
                  <div key={option.optionId}>
                    <label htmlFor={`opt-${option.optionId}`} className="mb-1.5 block text-xs text-ink-muted">
                      {option.optionName}
                    </label>
                    <Select
                      id={`opt-${option.optionId}`}
                      value={optionChoices[option.optionId] ?? ''}
                      onChange={(event) =>
                        setOptionChoices((current) => ({ ...current, [option.optionId]: event.target.value }))
                      }
                    >
                      <option value="" disabled>
                        Select {option.optionName.toLowerCase()}
                      </option>
                      {option.values.map((value) => (
                        <option key={value.optionValueId} value={value.optionValueId}>
                          {value.value}
                        </option>
                      ))}
                    </Select>
                  </div>
                ))}
                <FieldError message={fieldErrors.optionValueIds} />
              </div>
            ) : null}

            <div>
              <label htmlFor="sku-amount" className="mb-2 block text-sm text-ink">
                Amount <span className="text-red-500">*</span>
              </label>
              <Input
                id="sku-amount"
                type="number"
                step="0.01"
                min="0"
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
                placeholder="0.00"
              />
              <FieldError message={fieldErrors.amount} />
            </div>

            <div>
              <label htmlFor="sku-currency" className="mb-2 block text-sm text-ink">
                Currency <span className="text-red-500">*</span>
              </label>
              <Select id="sku-currency" value={currency} onChange={(event) => setCurrency(event.target.value)}>
                <option>USD</option>
                <option>EUR</option>
                <option>VND</option>
              </Select>
              <FieldError message={fieldErrors.currency} />
            </div>

            <div>
              <label htmlFor="sku-weight" className="mb-2 block text-sm text-ink">
                Weight Grams <span className="text-red-500">*</span>
              </label>
              <Input
                id="sku-weight"
                type="number"
                min="1"
                value={weightGrams}
                onChange={(event) => setWeightGrams(event.target.value)}
                placeholder="0"
              />
              <FieldError message={fieldErrors.weightGrams} />
            </div>

            {isEdit ? (
              <p className="text-xs text-ink-subtle">Product, code and option values are fixed after creation.</p>
            ) : null}

            <FormError message={formError ?? undefined} />
          </form>
        </SlideOver>
      ) : null}

      <ConfirmDialog
        open={confirmOpen}
        title="Deactivate SKU"
        message={`Deactivate “${editing?.skuCode ?? ''}”? It will be marked inactive.`}
        confirmLabel="Deactivate"
        pending={deactivateSku.isPending}
        onConfirm={submitDeactivate}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
