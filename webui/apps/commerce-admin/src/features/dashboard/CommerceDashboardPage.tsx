import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, CardHeader, CardTitle, IconInput, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, ArrowRightIcon, FilterIcon, FolderIcon, GridIcon, MoreVerticalIcon, PackageIcon, SearchIcon, TagIcon, PageHeader, StatCard, StatusBadge, Toolbar, TableFooter, TableState, EMPTY, formatAmount, formatCount } from '@ui';
import { CategoryTree, TreeLegend } from '../../components/CategoryTree';
import { useCatalogCounts, useCategoryTree, useNameLookups, useProducts, useSkus } from '../../api/queries';

const PREVIEW_SIZE = 5;

export function CommerceDashboardPage() {
  const [treeFilter, setTreeFilter] = useState('');
  const [productSearch, setProductSearch] = useState('');
  const [productStatus, setProductStatus] = useState('All Status');
  const [productPage, setProductPage] = useState(1);
  const [skuSearch, setSkuSearch] = useState('');
  const [skuStatus, setSkuStatus] = useState('All Status');
  const [skuPage, setSkuPage] = useState(1);

  const counts = useCatalogCounts();
  const { data: tree, isLoading: treeLoading } = useCategoryTree();
  const { categoryNames, brandNames } = useNameLookups();
  const products = useProducts({ page: productPage, size: PREVIEW_SIZE });
  const skus = useSkus({ page: skuPage, size: PREVIEW_SIZE });

  const visibleProducts = (products.data?.content ?? [])
    .filter((product) => productStatus === 'All Status' || (productStatus === 'Active') === product.isActive)
    .filter((product) => product.productName.toLowerCase().includes(productSearch.toLowerCase()));

  const visibleSkus = (skus.data?.content ?? [])
    .filter((sku) => skuStatus === 'All Status' || (skuStatus === 'Active') === sku.isActive)
    .filter((sku) => sku.skuCode.toLowerCase().includes(skuSearch.toLowerCase()));

  const stats = [
    { key: 'categories', label: 'Categories', value: counts.categories, icon: <FolderIcon size={20} />, tone: 'emerald' as const },
    { key: 'brands', label: 'Brands', value: counts.brands, icon: <TagIcon size={20} />, tone: 'sky' as const },
    { key: 'products', label: 'Products', value: counts.products, icon: <PackageIcon size={20} />, tone: 'amber' as const },
    { key: 'skus', label: 'SKUs', value: counts.skus, icon: <GridIcon size={20} />, tone: 'indigo' as const }
  ];

  return (
    <div className="px-8 py-7">
      <PageHeader title="Dashboard" subtitle="Overview of catalog and inventory health" />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map((stat) => (
          <StatCard
            key={stat.key}
            icon={stat.icon}
            label={stat.label}
            tone={stat.tone}
            value={counts.isLoading ? '…' : formatCount(stat.value)}
          />
        ))}
      </div>

      <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,372px)_minmax(0,1fr)]">
        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Category Tree</CardTitle>
            <Button size="sm" asChild>
              <Link to="/categories">Create Category</Link>
            </Button>
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
          <div className="scroll-slim min-h-0 flex-1 overflow-y-auto px-3 pb-2">
            {treeLoading ? (
              <p className="px-2 py-6 text-sm text-ink-subtle">Loading categories…</p>
            ) : (tree?.length ?? 0) === 0 ? (
              <p className="px-2 py-6 text-sm text-ink-subtle">No categories yet.</p>
            ) : (
              <CategoryTree nodes={tree ?? []} showIds filter={treeFilter} />
            )}
          </div>
          <TreeLegend className="border-t border-line px-5 py-3.5" />
        </Card>

        <Card className="flex flex-col">
          <CardHeader>
            <CardTitle>Product Catalog</CardTitle>
            <Button size="sm" asChild>
              <Link to="/products">New Product</Link>
            </Button>
          </CardHeader>
          <div className="px-5 py-4">
            <Toolbar
              search={productSearch}
              onSearchChange={setProductSearch}
              searchPlaceholder="Search products..."
              filters={[
                {
                  id: 'status',
                  value: productStatus,
                  onChange: setProductStatus,
                  options: ['All Status', 'Active', 'Inactive']
                }
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
                <TableHead className="w-9 px-2" />
              </tr>
            </TableHeader>
            <TableBody>
              <TableState
                colSpan={6}
                isLoading={products.isLoading}
                isError={products.isError}
                isEmpty={visibleProducts.length === 0}
                entity="products"
              />
              {visibleProducts.map((product) => (
                <TableRow key={product.productId}>
                  <TableCell className="whitespace-nowrap font-mono text-[11px]">{product.productId}</TableCell>
                  <TableCell className="text-ink">{product.productName}</TableCell>
                  <TableCell className="whitespace-nowrap">{categoryNames.get(product.categoryId) ?? EMPTY}</TableCell>
                  <TableCell>{brandNames.get(product.brandId) ?? EMPTY}</TableCell>
                  <TableCell>
                    <StatusBadge isActive={product.isActive} />
                  </TableCell>
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
            from={visibleProducts.length === 0 ? 0 : (productPage - 1) * PREVIEW_SIZE + 1}
            to={(productPage - 1) * PREVIEW_SIZE + visibleProducts.length}
            total={products.data?.totalElements ?? 0}
            noun="products"
            page={productPage}
            totalPages={Math.max(products.data?.totalPages ?? 1, 1)}
            onPageChange={setProductPage}
          />
        </Card>
      </div>

      <Card className="mt-5 flex flex-col">
        <CardHeader className="flex-wrap">
          <CardTitle className="mr-auto">Inventory by SKU</CardTitle>
          <Toolbar
            className="gap-2"
            search={skuSearch}
            onSearchChange={setSkuSearch}
            searchPlaceholder="Search SKUs..."
            filters={[
              { id: 'status', value: skuStatus, onChange: setSkuStatus, options: ['All Status', 'Active', 'Inactive'] }
            ]}
            trailing={
              <Button size="sm" asChild>
                <Link to="/skus">Add SKU</Link>
              </Button>
            }
          />
        </CardHeader>
        <Table>
          <TableHeader>
            <tr>
              <TableHead>SKU Code</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Currency</TableHead>
              <TableHead>Weight (g)</TableHead>
              <TableHead>Status</TableHead>
            </tr>
          </TableHeader>
          <TableBody>
            <TableState
              colSpan={5}
              isLoading={skus.isLoading}
              isError={skus.isError}
              isEmpty={visibleSkus.length === 0}
              entity="SKUs"
            />
            {visibleSkus.map((sku) => (
              <TableRow key={sku.skuId}>
                <TableCell className="whitespace-nowrap font-mono text-[11px]">{sku.skuCode}</TableCell>
                <TableCell>{formatAmount(sku.amount)}</TableCell>
                <TableCell>{sku.currency}</TableCell>
                <TableCell>{sku.weightGrams}</TableCell>
                <TableCell>
                  <StatusBadge isActive={sku.isActive} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <TableFooter
          from={visibleSkus.length === 0 ? 0 : (skuPage - 1) * PREVIEW_SIZE + 1}
          to={(skuPage - 1) * PREVIEW_SIZE + visibleSkus.length}
          total={skus.data?.totalElements ?? 0}
          noun="SKUs"
          page={skuPage}
          totalPages={Math.max(skus.data?.totalPages ?? 1, 1)}
          onPageChange={setSkuPage}
        />
        <div className="border-t border-line px-5 py-3.5">
          <Link to="/brands" className="inline-flex items-center gap-2 text-sm font-medium text-brand hover:underline">
            View all brands
            <ArrowRightIcon size={15} />
          </Link>
        </div>
      </Card>
    </div>
  );
}
