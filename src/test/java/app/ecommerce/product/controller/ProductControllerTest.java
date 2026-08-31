package app.ecommerce.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.ecommerce.product.api.dto.request.CreateProductRequest;
import app.ecommerce.shared.api.dto.response.PageResponse;
import app.ecommerce.product.api.dto.response.ProductResponse;
import app.ecommerce.product.api.service.ProductService;
import app.ecommerce.product.impl.controller.ProductController;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createsProductAndReturnsItsCanonicalLocation() throws Exception {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-30T10:00:00Z");
        when(productService.createProduct(any(CreateProductRequest.class)))
            .thenReturn(new ProductResponse(productId, categoryId, "Laptop", "desc", true, now, now));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId":"%s","productName":"Laptop","productDescription":"desc"}
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/products/" + productId))
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.productName").value("Laptop"));
    }

    @Test
    void rejectsBlankProductName() throws Exception {
        final var categoryId = UUID.randomUUID();
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryId":"%s","productName":"   "}
                    """.formatted(categoryId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("productName"));
    }

    @Test
    void rejectsMissingCategoryId() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productName":"Laptop"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("categoryId"));
    }

    @Test
    void getsProductDetail() throws Exception {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-30T10:00:00Z");
        when(productService.getProduct(productId))
            .thenReturn(new ProductResponse(productId, categoryId, "Laptop", "desc", true, now, now));

        mockMvc.perform(get("/api/products/{productId}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.productName").value("Laptop"));
    }

    @Test
    void rejectsMalformedProductId() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PATH_PARAMETER"));

        verifyNoInteractions(productService);
    }

    @Test
    void getsProductsUsingRequestedPagination() throws Exception {
        final var productId = UUID.randomUUID();
        final var categoryId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-30T10:00:00Z");
        final var product =
            new ProductResponse(productId, categoryId, "Laptop", "desc", true, now, now);
        when(productService.getProducts(categoryId, 2, 5)).thenReturn(new PageResponse<>(
            List.of(product), 2, 5, 1, 6, 2, false, true, false, true));

        mockMvc.perform(get("/api/products")
                .queryParam("categoryId", categoryId.toString())
                .queryParam("page", "2")
                .queryParam("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].productId").value(productId.toString()))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(productService).getProducts(categoryId, 2, 5);
    }

    @Test
    void rejectsMissingCategoryIdOnList() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }
}
