package app.ecommerce.product.impl.controller;

import app.ecommerce.product.api.dto.response.ProductImageResponse;
import app.ecommerce.product.api.dto.response.ProductOptionResponse;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.product.api.service.ProductImageService;
import app.ecommerce.product.api.service.ProductOptionService;
import app.ecommerce.product.api.service.ProductService;
import app.ecommerce.shared.api.dto.response.PageResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/products")
public class PublicProductController {

    private final ProductService productService;
    private final ProductOptionService productOptionService;
    private final ProductImageService productImageService;

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

    @GetMapping("/{productId}/options")
    public List<ProductOptionResponse> getOptions(@PathVariable final UUID productId) {
        return productOptionService.getProductOptions(productId);
    }

    @GetMapping("/{productId}/images")
    public List<ProductImageResponse> getImages(@PathVariable final UUID productId) {
        return productImageService.getProductImages(productId);
    }
}
