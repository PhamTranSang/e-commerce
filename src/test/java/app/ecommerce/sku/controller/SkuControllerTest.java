package app.ecommerce.sku.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.sku.api.dto.request.CreateSkuRequest;
import app.ecommerce.sku.api.dto.response.SkuResponse;
import app.ecommerce.sku.api.service.SkuService;
import app.ecommerce.sku.impl.controller.SkuController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SkuController.class)
class SkuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkuService skuService;

    @Test
    void createsSkuAndReturnsItsCanonicalLocation() throws Exception {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-31T10:00:00Z");
        when(skuService.createSku(any(CreateSkuRequest.class)))
            .thenReturn(new SkuResponse(
                skuId, productId, "SKU-001", 500, new BigDecimal("19.99"), "USD", true, now, now));

        mockMvc.perform(post("/api/skus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productId":"%s","skuCode":"SKU-001","weightGrams":500,\
                    "amount":19.99,"currency":"USD"}
                    """.formatted(productId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/skus/" + skuId))
            .andExpect(jsonPath("$.skuId").value(skuId.toString()))
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.skuCode").value("SKU-001"));
    }

    @Test
    void rejectsBlankSkuCode() throws Exception {
        final var productId = UUID.randomUUID();
        mockMvc.perform(post("/api/skus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productId":"%s","skuCode":"   ","weightGrams":500,\
                    "amount":19.99,"currency":"USD"}
                    """.formatted(productId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("skuCode"));
    }

    @Test
    void rejectsMissingProductId() throws Exception {
        mockMvc.perform(post("/api/skus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skuCode":"SKU-001","weightGrams":500,"amount":19.99,"currency":"USD"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("productId"));
    }

    @Test
    void rejectsInvalidCurrency() throws Exception {
        final var productId = UUID.randomUUID();
        mockMvc.perform(post("/api/skus")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productId":"%s","skuCode":"SKU-001","weightGrams":500,\
                    "amount":19.99,"currency":"usd"}
                    """.formatted(productId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("currency"));
    }

    @Test
    void getsSkuDetail() throws Exception {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-31T10:00:00Z");
        when(skuService.getSku(skuId))
            .thenReturn(new SkuResponse(
                skuId, productId, "SKU-001", 500, new BigDecimal("19.99"), "USD", true, now, now));

        mockMvc.perform(get("/api/skus/{skuId}", skuId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skuId").value(skuId.toString()))
            .andExpect(jsonPath("$.skuCode").value("SKU-001"));
    }

    @Test
    void rejectsMalformedSkuId() throws Exception {
        mockMvc.perform(get("/api/skus/{skuId}", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PATH_PARAMETER"));

        verifyNoInteractions(skuService);
    }

    @Test
    void getsSkusUsingRequestedPagination() throws Exception {
        final var skuId = UUID.randomUUID();
        final var productId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-31T10:00:00Z");
        final var sku = new SkuResponse(
            skuId, productId, "SKU-001", 500, new BigDecimal("19.99"), "USD", true, now, now);
        when(skuService.getSkus(productId, 2, 5)).thenReturn(new PageResponse<>(
            List.of(sku), 2, 5, 1, 6, 2, false, true, false, true));

        mockMvc.perform(get("/api/skus")
                .queryParam("productId", productId.toString())
                .queryParam("page", "2")
                .queryParam("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].skuId").value(skuId.toString()))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(skuService).getSkus(productId, 2, 5);
    }

    @Test
    void rejectsMissingProductIdOnList() throws Exception {
        mockMvc.perform(get("/api/skus"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(skuService);
    }
}
