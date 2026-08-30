package app.ecommerce.catalog.impl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.ecommerce.catalog.api.dto.request.CreateCategoryRequest;
import app.ecommerce.catalog.api.dto.response.CategoryResponse;
import app.ecommerce.catalog.api.dto.response.PageResponse;
import app.ecommerce.catalog.api.service.CategoryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void createsCategoryAndReturnsItsCanonicalLocation() throws Exception {
        final var categoryId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-17T10:00:00Z");
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
            .thenReturn(new CategoryResponse(categoryId, "Electronics", true, now, now));

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryName":"Electronics"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/categories/" + categoryId))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.categoryName").value("Electronics"))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void rejectsBlankCategoryName() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryName":"   "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("categoryName"));
    }

    @Test
    void rejectsMalformedCreateCategoryBody() throws Exception {
        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"categoryName":}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    void getsCategoryDetail() throws Exception {
        final var categoryId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-17T10:00:00Z");
        when(categoryService.getCategory(categoryId))
            .thenReturn(new CategoryResponse(categoryId, "Electronics", true, now, now));

        mockMvc.perform(get("/api/categories/{categoryId}", categoryId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.categoryName").value("Electronics"))
            .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void rejectsMalformedCategoryId() throws Exception {
        mockMvc.perform(get("/api/categories/{categoryId}", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PATH_PARAMETER"));

        verifyNoInteractions(categoryService);
    }

    @Test
    void getsCategoriesUsingRequestedPagination() throws Exception {
        final var categoryId = UUID.randomUUID();
        final var now = Instant.parse("2026-08-17T10:00:00Z");
        final var category = new CategoryResponse(
            categoryId,
            "Electronics",
            true,
            now,
            now
        );
        when(categoryService.getCategories(2, 5)).thenReturn(new PageResponse<>(
            List.of(category),
            2,
            5,
            1,
            6,
            2,
            false,
            true,
            false,
            true
        ));

        mockMvc.perform(get("/api/categories")
                .queryParam("page", "2")
                .queryParam("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.content[0].categoryName").value("Electronics"))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(categoryService).getCategories(2, 5);
    }

    @Test
    void rejectsPageBelowOne() throws Exception {
        mockMvc.perform(get("/api/categories")
                .queryParam("page", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("page"))
            .andExpect(jsonPath("$.errors[0].message")
                .value("Page must be greater than or equal to 1"));
    }
}
