package app.ecommerce.brand.api.service;

import app.ecommerce.brand.api.dto.request.CreateBrandRequest;
import app.ecommerce.brand.api.dto.request.RenameBrandRequest;
import app.ecommerce.brand.api.dto.response.BrandResponse;
import app.ecommerce.shared.api.dto.response.PageResponse;
import java.util.UUID;

public interface BrandService {

    BrandResponse createBrand(final CreateBrandRequest request);

    BrandResponse getBrand(final UUID brandId);

    PageResponse<BrandResponse> getBrands(final int page, final int size);

    BrandResponse renameBrand(final UUID brandId, final RenameBrandRequest request);

    void deactivateBrand(final UUID brandId);
}
