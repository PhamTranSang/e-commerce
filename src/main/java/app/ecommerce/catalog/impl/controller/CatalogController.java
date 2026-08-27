package app.ecommerce.catalog.impl.controller;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.request.RenameCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.api.dto.response.PageResponse;
import app.ecommerce.catalog.api.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CatalogController {

    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        final var response = service.createCategory(request);
        final var location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{categoryId}")
            .buildAndExpand(response.categoryId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable final UUID categoryId) {
        final var response = service.getCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> getCategories(
        @RequestParam(defaultValue = "1")
        @Min(value = 1, message = "Page must be greater than or equal to 1")
        final int page,
        @RequestParam(defaultValue = "10")
        @Min(value = 1, message = "Size must be greater than or equal to 1")
        @Max(value = 100, message = "Size must not exceed 100")
        final int size
    ) {
        final var response = service.getCategories(page, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> renameCategory(
        @PathVariable final UUID categoryId,
        @Valid @RequestBody final RenameCategoryRequest request
    ) {
        final var response = service.renameCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivateCategory(
        @PathVariable final UUID categoryId
    ) {
        service.deactivateCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
