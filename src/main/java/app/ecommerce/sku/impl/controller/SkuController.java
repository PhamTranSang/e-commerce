package app.ecommerce.sku.impl.controller;

import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.service.SkuService;
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
@RequestMapping("/api/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService service;

    @PostMapping
    public ResponseEntity<SkuResponse> createSku(
            @Valid @RequestBody final CreateSkuRequest request) {
        final var response = service.createSku(request);
        final var location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{skuId}")
            .buildAndExpand(response.skuId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{skuId}")
    public ResponseEntity<SkuResponse> getSku(@PathVariable final UUID skuId) {
        final var response = service.getSku(skuId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SkuResponse>> getSkus(
        @RequestParam final UUID productId,
        @RequestParam(defaultValue = "1")
        @Min(value = 1, message = "Page must be greater than or equal to 1")
        final int page,
        @RequestParam(defaultValue = "10")
        @Min(value = 1, message = "Size must be greater than or equal to 1")
        @Max(value = 100, message = "Size must not exceed 100")
        final int size
    ) {
        final var response = service.getSkus(productId, page, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{skuId}")
    public ResponseEntity<SkuResponse> updateSku(
        @PathVariable final UUID skuId,
        @Valid @RequestBody final UpdateSkuRequest request
    ) {
        final var response = service.updateSku(skuId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{skuId}")
    public ResponseEntity<Void> deactivateSku(@PathVariable final UUID skuId) {
        service.deactivateSku(skuId);
        return ResponseEntity.noContent().build();
    }
}
