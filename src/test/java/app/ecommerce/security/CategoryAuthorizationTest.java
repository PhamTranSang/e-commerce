package app.ecommerce.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.ecommerce.catalog.api.service.CategoryService;
import app.ecommerce.catalog.impl.controller.AdminCategoryController;
import app.ecommerce.catalog.impl.controller.PublicCategoryController;
import app.ecommerce.security.impl.config.ProblemDetailAccessDeniedHandler;
import app.ecommerce.security.impl.config.ProblemDetailAuthenticationEntryPoint;
import app.ecommerce.security.impl.config.SecurityConfig;
import app.ecommerce.security.impl.config.SecurityProblemDetailWriter;
import app.ecommerce.security.impl.filter.JwtAuthenticationFilter;
import app.ecommerce.security.impl.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AdminCategoryController.class, PublicCategoryController.class})
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    ProblemDetailAuthenticationEntryPoint.class,
    ProblemDetailAccessDeniedHandler.class,
    SecurityProblemDetailWriter.class
})
class CategoryAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void adminEndpointRejectsAnonymousWith401() throws Exception {
        mockMvc.perform(get("/api/admin/categories/tree"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointRejectsNonStaffRoleWith403() throws Exception {
        mockMvc.perform(get("/api/admin/categories/tree").with(user("buyer").roles("BUYER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointAllowsAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/categories/tree").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void adminEndpointAllowsStaffRole() throws Exception {
        mockMvc.perform(get("/api/admin/categories/tree").with(user("staff").roles("STAFF")))
            .andExpect(status().isOk());
    }

    @Test
    void publicEndpointIsOpenToAnonymous() throws Exception {
        mockMvc.perform(get("/api/public/categories/tree"))
            .andExpect(status().isOk());
    }
}
