package app.ecommerce.catalog.api.service;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.api.dto.response.PageResponse;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(final CreateCategoryRequest request);

    CategoryResponse getCategory(final UUID categoryId);

    PageResponse<CategoryResponse> getCategories(final int page, final int size);

    CategoryResponse renameCategory(
        final UUID categoryId,
        final RenameCategoryRequest request
    );

    void deactivateCategory(final UUID categoryId);
}
