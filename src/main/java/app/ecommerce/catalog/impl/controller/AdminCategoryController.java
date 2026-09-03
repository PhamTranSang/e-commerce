package app.ecommerce.catalog.impl.controller;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.api.dto.response.CategoryTreeResponse;
import app.ecommerce.catalog.api.service.CategoryService;
import app.ecommerce.shared.api.dto.response.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public CategoryResponse create(@Valid @RequestBody final CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse get(@PathVariable final UUID categoryId) {
        return categoryService.getCategory(categoryId);
    }

    @GetMapping
    public PageResponse<CategoryResponse> list(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return categoryService.getCategories(page, size);
    }

    @GetMapping("/tree")
    public List<CategoryTreeResponse> tree() {
        return categoryService.getCategoryTree();
    }

    @PatchMapping("/{categoryId}")
    public CategoryResponse rename(
            @PathVariable final UUID categoryId,
            @Valid @RequestBody final RenameCategoryRequest request) {
        return categoryService.renameCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public void deactivate(@PathVariable final UUID categoryId) {
        categoryService.deactivateCategory(categoryId);
    }
}
