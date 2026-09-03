package app.ecommerce.sku.impl.controller;

import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.service.SkuService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/skus")
public class AdminSkuController {

    private final SkuService skuService;

    @PostMapping
    public SkuResponse create(@Valid @RequestBody final CreateSkuRequest request) {
        return skuService.createSku(request);
    }

    @GetMapping("/{skuId}")
    public SkuResponse get(@PathVariable final UUID skuId) {
        return skuService.getSku(skuId);
    }

    @GetMapping
    public PageResponse<SkuResponse> list(
            @RequestParam(required = false) final UUID productId,
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return skuService.getSkus(productId, page, size);
    }

    @PatchMapping("/{skuId}")
    public SkuResponse update(
            @PathVariable final UUID skuId,
            @Valid @RequestBody final UpdateSkuRequest request) {
        return skuService.updateSku(skuId, request);
    }

    @DeleteMapping("/{skuId}")
    public void deactivate(@PathVariable final UUID skuId) {
        skuService.deactivateSku(skuId);
    }
}
