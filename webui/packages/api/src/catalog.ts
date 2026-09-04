import type {
  BrandResponse,
  CategoryResponse,
  CategoryTreeResponse,
  CreateBrandRequest,
  CreateCategoryRequest,
  CreateProductRequest,
  CreateSkuRequest,
  PageResponse,
  ProductImageResponse,
  ProductOptionResponse,
  ProductResponse,
  RenameBrandRequest,
  RenameCategoryRequest,
  SkuResponse,
  UpdateProductRequest,
  UpdateSkuRequest
} from '@domain/index';
import { http } from './http';

/** The backend pages are 1-based (`@RequestParam(defaultValue = "1")`). */
export type PageParams = { page?: number; size?: number };

async function getPage<T>(url: string, params: PageParams): Promise<PageResponse<T>> {
  const { data } = await http.get<PageResponse<T>>(url, {
    params: { page: params.page ?? 1, size: params.size ?? 20 }
  });
  return data;
}

export const listCategories = (params: PageParams = {}) =>
  getPage<CategoryResponse>('/api/admin/categories', params);

export const listBrands = (params: PageParams = {}) => getPage<BrandResponse>('/api/admin/brands', params);

export const listProducts = (params: PageParams = {}) => getPage<ProductResponse>('/api/admin/products', params);

export const listSkus = (params: PageParams = {}) => getPage<SkuResponse>('/api/admin/skus', params);

export async function fetchCategoryTree(): Promise<CategoryTreeResponse[]> {
  const { data } = await http.get<CategoryTreeResponse[]>('/api/admin/categories/tree');
  return data;
}

export async function fetchProductOptions(productId: string): Promise<ProductOptionResponse[]> {
  const { data } = await http.get<ProductOptionResponse[]>(`/api/admin/products/${productId}/options`);
  return data;
}

export async function fetchProductImages(productId: string): Promise<ProductImageResponse[]> {
  const { data } = await http.get<ProductImageResponse[]>(`/api/admin/products/${productId}/images`);
  return data;
}

// --- Categories ---
export const createCategory = (body: CreateCategoryRequest) =>
  http.post<CategoryResponse>('/api/admin/categories', body).then((r) => r.data);
export const renameCategory = (categoryId: string, body: RenameCategoryRequest) =>
  http.patch<CategoryResponse>(`/api/admin/categories/${categoryId}`, body).then((r) => r.data);
export const deactivateCategory = (categoryId: string) =>
  http.delete<void>(`/api/admin/categories/${categoryId}`).then((r) => r.data);

// --- Brands ---
export const createBrand = (body: CreateBrandRequest) =>
  http.post<BrandResponse>('/api/admin/brands', body).then((r) => r.data);
export const renameBrand = (brandId: string, body: RenameBrandRequest) =>
  http.patch<BrandResponse>(`/api/admin/brands/${brandId}`, body).then((r) => r.data);
export const deactivateBrand = (brandId: string) =>
  http.delete<void>(`/api/admin/brands/${brandId}`).then((r) => r.data);

// --- Products ---
export const createProduct = (body: CreateProductRequest) =>
  http.post<ProductResponse>('/api/admin/products', body).then((r) => r.data);
export const updateProduct = (productId: string, body: UpdateProductRequest) =>
  http.patch<ProductResponse>(`/api/admin/products/${productId}`, body).then((r) => r.data);
export const deactivateProduct = (productId: string) =>
  http.delete<void>(`/api/admin/products/${productId}`).then((r) => r.data);

// --- SKUs ---
export const createSku = (body: CreateSkuRequest) =>
  http.post<SkuResponse>('/api/admin/skus', body).then((r) => r.data);
export const updateSku = (skuId: string, body: UpdateSkuRequest) =>
  http.patch<SkuResponse>(`/api/admin/skus/${skuId}`, body).then((r) => r.data);
export const deactivateSku = (skuId: string) =>
  http.delete<void>(`/api/admin/skus/${skuId}`).then((r) => r.data);
