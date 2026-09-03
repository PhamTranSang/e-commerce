package app.ecommerce.brand.impl.controller;

import app.ecommerce.brand.api.dto.request.CreateBrandRequest;
import app.ecommerce.brand.api.dto.request.RenameBrandRequest;
import app.ecommerce.brand.api.dto.response.BrandResponse;
import app.ecommerce.brand.api.service.BrandService;
import app.ecommerce.shared.api.dto.response.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/brands")
public class AdminBrandController {

    private final BrandService brandService;

    @PostMapping
    public BrandResponse create(@Valid @RequestBody final CreateBrandRequest request) {
        return brandService.createBrand(request);
    }

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

    @PatchMapping("/{brandId}")
    public BrandResponse rename(
            @PathVariable final UUID brandId,
            @Valid @RequestBody final RenameBrandRequest request) {
        return brandService.renameBrand(brandId, request);
    }

    @DeleteMapping("/{brandId}")
    public void deactivate(@PathVariable final UUID brandId) {
        brandService.deactivateBrand(brandId);
    }
}
