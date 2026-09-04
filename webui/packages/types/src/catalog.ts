/** Mirrors app.ecommerce.*.api.dto.response.* — UUIDs arrive as strings, Instants as ISO-8601. */

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type CategoryResponse = {
  categoryId: string;
  parentId: string | null;
  categoryName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CategoryTreeResponse = {
  categoryId: string;
  categoryName: string;
  children: CategoryTreeResponse[];
};

export type BrandResponse = {
  brandId: string;
  brandName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ProductResponse = {
  productId: string;
  categoryId: string;
  brandId: string;
  productName: string;
  productDescription: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ProductOptionValueResponse = {
  optionValueId: string;
  value: string;
  position: number;
};

export type ProductOptionResponse = {
  optionId: string;
  optionName: string;
  position: number;
  values: ProductOptionValueResponse[];
};

export type ProductImageResponse = {
  imageId: string;
  productId: string;
  optionValueId: string | null;
  url: string;
  altText: string | null;
  position: number;
  isPrimary: boolean;
  createdAt: string;
  updatedAt: string;
};

export type SkuResponse = {
  skuId: string;
  productId: string;
  skuCode: string;
  amount: string;
  currency: string;
  weightGrams: number;
  optionValueIds: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

/** RFC 7807 problem body used by the backend's GlobalExceptionHandler. */
export type ValidationError = { field: string; message: string };
export type ProblemDetail = {
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  errors?: ValidationError[];
  correlationId?: string;
};

export type CreateBrandRequest = { brandName: string };
export type RenameBrandRequest = { brandName: string };

export type CreateCategoryRequest = { categoryName: string; parentId?: string | null };
export type RenameCategoryRequest = { categoryName: string };

export type CreateProductRequest = {
  categoryId: string;
  brandId: string;
  productName: string;
  productDescription?: string;
};
export type UpdateProductRequest = { productName: string; productDescription?: string };

export type CreateSkuRequest = {
  productId: string;
  skuCode: string;
  amount: string;
  currency: string;
  weightGrams: number;
  optionValueIds: string[];
};
export type UpdateSkuRequest = { amount: string; currency: string; weightGrams: number };
