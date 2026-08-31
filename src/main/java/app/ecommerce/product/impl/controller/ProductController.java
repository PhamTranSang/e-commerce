package app.ecommerce.product.impl.controller;

import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.product.api.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody final CreateProductRequest request) {
        final var response = service.createProduct(request);
        final var location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{productId}")
            .buildAndExpand(response.productId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable final UUID productId) {
        final var response = service.getProduct(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
        @RequestParam final UUID categoryId,
        @RequestParam(defaultValue = "1")
        @Min(value = 1, message = "Page must be greater than or equal to 1")
        final int page,
        @RequestParam(defaultValue = "10")
        @Min(value = 1, message = "Size must be greater than or equal to 1")
        @Max(value = 100, message = "Size must not exceed 100")
        final int size
    ) {
        final var response = service.getProducts(categoryId, page, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable final UUID productId,
        @Valid @RequestBody final UpdateProductRequest request
    ) {
        final var response = service.updateProduct(productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable final UUID productId) {
        service.deactivateProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
