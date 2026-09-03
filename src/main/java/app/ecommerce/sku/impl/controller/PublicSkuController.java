package app.ecommerce.sku.impl.controller;

import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.service.SkuService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/skus")
public class PublicSkuController {

    private final SkuService skuService;

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
}
