package app.ecommerce.brand.impl.controller;

import app.ecommerce.brand.api.dto.response.BrandResponse;
import app.ecommerce.brand.api.service.BrandService;
import app.ecommerce.shared.api.dto.response.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/brands")
public class PublicBrandController {

    private final BrandService brandService;

    @GetMapping("/{brandId}")
    public BrandResponse get(@PathVariable final UUID brandId) {
        return brandService.getBrand(brandId);
    }

    @GetMapping
    public PageResponse<BrandResponse> list(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return brandService.getBrands(page, size);
    }
}
