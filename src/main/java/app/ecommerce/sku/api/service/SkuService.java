package app.ecommerce.sku.api.service;

import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.request.UpdateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import java.util.UUID;

public interface SkuService {

    SkuResponse createSku(final CreateSkuRequest request);

    SkuResponse getSku(final UUID skuId);

    PageResponse<SkuResponse> getSkus(final UUID productId, final int page, final int size);

    SkuResponse updateSku(final UUID skuId, final UpdateSkuRequest request);

    void deactivateSku(final UUID skuId);
}
