package app.ecommerce.catalog.api.service;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.api.dto.response.CategoryTreeResponse;
import app.ecommerce.shared.api.dto.response.PageResponse;
import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(final CreateCategoryRequest request);

    CategoryResponse getCategory(final UUID categoryId);

    List<CategoryTreeResponse> getCategoryTree();

    PageResponse<CategoryResponse> getCategories(final int page, final int size);

    CategoryResponse renameCategory(final UUID categoryId, final RenameCategoryRequest request);

    void deactivateCategory(final UUID categoryId);
}
