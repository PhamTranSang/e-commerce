import { useQueries, useQuery } from '@tanstack/react-query';
import {
  fetchCategoryTree,
  fetchProductOptions,
  listBrands,
  listCategories,
  listProducts,
  listSkus,
  type PageParams
} from '@api';

/** Large enough to resolve id → name for the current page without paging twice. */
const LOOKUP_SIZE = 200;

export const catalogKeys = {
  categories: (params: PageParams) => ['categories', params] as const,
  categoryTree: () => ['categories', 'tree'] as const,
  brands: (params: PageParams) => ['brands', params] as const,
  products: (params: PageParams) => ['products', params] as const,
  skus: (params: PageParams) => ['skus', params] as const,
  productOptions: (productId: string) => ['products', productId, 'options'] as const
};

export const useCategories = (params: PageParams) =>
  useQuery({ queryKey: catalogKeys.categories(params), queryFn: () => listCategories(params) });

export const useCategoryTree = () =>
  useQuery({ queryKey: catalogKeys.categoryTree(), queryFn: fetchCategoryTree });

export const useBrands = (params: PageParams) =>
  useQuery({ queryKey: catalogKeys.brands(params), queryFn: () => listBrands(params) });

export const useProducts = (params: PageParams) =>
  useQuery({ queryKey: catalogKeys.products(params), queryFn: () => listProducts(params) });

export const useSkus = (params: PageParams) =>
  useQuery({ queryKey: catalogKeys.skus(params), queryFn: () => listSkus(params) });

export const useProductOptions = (productId: string | null) =>
  useQuery({
    queryKey: catalogKeys.productOptions(productId ?? ''),
    queryFn: () => fetchProductOptions(productId!),
    enabled: Boolean(productId)
  });

/** Products only carry categoryId/brandId, so the tables join the names client-side. */
export function useNameLookups() {
  const params = { page: 1, size: LOOKUP_SIZE };
  const [categories, brands] = useQueries({
    queries: [
      { queryKey: catalogKeys.categories(params), queryFn: () => listCategories(params) },
      { queryKey: catalogKeys.brands(params), queryFn: () => listBrands(params) }
    ]
  });

  const categoryNames = new Map((categories.data?.content ?? []).map((c) => [c.categoryId, c.categoryName]));
  const brandNames = new Map((brands.data?.content ?? []).map((b) => [b.brandId, b.brandName]));

  return { categoryNames, brandNames, isLoading: categories.isLoading || brands.isLoading };
}

/** Dashboard totals come from each list's `totalElements`; size 1 keeps the payload tiny. */
export function useCatalogCounts() {
  const params = { page: 1, size: 1 };
  const results = useQueries({
    queries: [
      { queryKey: catalogKeys.categories(params), queryFn: () => listCategories(params) },
      { queryKey: catalogKeys.brands(params), queryFn: () => listBrands(params) },
      { queryKey: catalogKeys.products(params), queryFn: () => listProducts(params) },
      { queryKey: catalogKeys.skus(params), queryFn: () => listSkus(params) }
    ]
  });

  const [categories, brands, products, skus] = results;
  return {
    categories: categories.data?.totalElements,
    brands: brands.data?.totalElements,
    products: products.data?.totalElements,
    skus: skus.data?.totalElements,
    isLoading: results.some((result) => result.isLoading),
    isError: results.some((result) => result.isError)
  };
}