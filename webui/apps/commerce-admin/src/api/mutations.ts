import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createBrand,
  createCategory,
  createProduct,
  createSku,
  deactivateBrand,
  deactivateCategory,
  deactivateProduct,
  deactivateSku,
  renameBrand,
  renameCategory,
  updateProduct,
  updateSku
} from '@api';

/**
 * All catalog writes invalidate the matching query family so lists/trees refetch.
 * Broad keys (['brands'], ['categories', …]) are intentional — correctness over
 * shaving a request, given the modest page sizes here.
 */
function useCatalogMutation<TArgs, TResult>(
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

const BRANDS = [['brands']] as const;
const CATEGORIES = [['categories']] as const;
const PRODUCTS = [['products']] as const;
const SKUS = [['skus']] as const;

// --- Brands ---
export const useCreateBrand = () => useCatalogMutation(createBrand, BRANDS);
export const useRenameBrand = () =>
  useCatalogMutation((args: { brandId: string; brandName: string }) => renameBrand(args.brandId, { brandName: args.brandName }), BRANDS);
export const useDeactivateBrand = () => useCatalogMutation(deactivateBrand, BRANDS);

// --- Categories (tree + list share the ['categories'] prefix) ---
export const useCreateCategory = () => useCatalogMutation(createCategory, CATEGORIES);
export const useRenameCategory = () =>
  useCatalogMutation((args: { categoryId: string; categoryName: string }) => renameCategory(args.categoryId, { categoryName: args.categoryName }), CATEGORIES);
export const useDeactivateCategory = () => useCatalogMutation(deactivateCategory, CATEGORIES);

// --- Products ---
export const useCreateProduct = () => useCatalogMutation(createProduct, PRODUCTS);
export const useUpdateProduct = () =>
  useCatalogMutation(
    (args: { productId: string; productName: string; productDescription?: string }) =>
      updateProduct(args.productId, { productName: args.productName, productDescription: args.productDescription }),
    PRODUCTS
  );
export const useDeactivateProduct = () => useCatalogMutation(deactivateProduct, PRODUCTS);

// --- SKUs ---
export const useCreateSku = () => useCatalogMutation(createSku, SKUS);
export const useUpdateSku = () =>
  useCatalogMutation(
    (args: { skuId: string; amount: string; currency: string; weightGrams: number }) =>
      updateSku(args.skuId, { amount: args.amount, currency: args.currency, weightGrams: args.weightGrams }),
    SKUS
  );
export const useDeactivateSku = () => useCatalogMutation(deactivateSku, SKUS);