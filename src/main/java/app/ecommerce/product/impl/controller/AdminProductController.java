package app.ecommerce.product.impl.controller;

import app.ecommerce.product.api.dto.request.CreateProductImageRequest;
import app.ecommerce.product.api.dto.request.CreateProductOptionRequest;
import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.product.api.dto.request.UpdateProductRequest;
import app.ecommerce.product.api.dto.response.ProductImageResponse;
import app.ecommerce.product.api.dto.response.ProductOptionResponse;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.product.api.service.ProductImageService;
import app.ecommerce.product.api.service.ProductOptionService;
import app.ecommerce.product.api.service.ProductService;
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
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final ProductOptionService productOptionService;
    private final ProductImageService productImageService;

    @PostMapping
    public ProductResponse create(@Valid @RequestBody final CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping("/{productId}")
    public ProductResponse get(@PathVariable final UUID productId) {
        return productService.getProduct(productId);
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) final UUID categoryId,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return productService.getProducts(categoryId, page, size);
    }

    @PatchMapping("/{productId}")
    public ProductResponse update(
            @PathVariable final UUID productId,
            @Valid @RequestBody final UpdateProductRequest request) {
        return productService.updateProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    public void deactivate(@PathVariable final UUID productId) {
        productService.deactivateProduct(productId);
    }

    @PostMapping("/{productId}/options")
    public ProductOptionResponse addOption(
            @PathVariable final UUID productId,
            @Valid @RequestBody final CreateProductOptionRequest request) {
        return productOptionService.addOption(productId, request);
    }

    @GetMapping("/{productId}/options")
    public List<ProductOptionResponse> getOptions(@PathVariable final UUID productId) {
        return productOptionService.getProductOptions(productId);
    }

    @PostMapping("/{productId}/images")
    public ProductImageResponse addImage(
            @PathVariable final UUID productId,
            @Valid @RequestBody final CreateProductImageRequest request) {
        return productImageService.addImage(productId, request);
    }

    @GetMapping("/{productId}/images")
    public List<ProductImageResponse> getImages(@PathVariable final UUID productId) {
        return productImageService.getProductImages(productId);
    }
}
