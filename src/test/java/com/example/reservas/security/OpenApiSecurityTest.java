package com.example.reservas.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiSecurityTest {

    @WebMvcTest
    @ContextConfiguration(classes = OpenApiSecurityTest.ProdProfileTest.ProdMvcTestConfig.class)
    @ActiveProfiles("prod")
    @AutoConfigureMockMvc(addFilters = true)
    static class ProdProfileTest {

        @SpringBootConfiguration
        @EnableAutoConfiguration
        @Import({SecurityConfig.class, JwtFilter.class})
        static class ProdMvcTestConfig {
        }

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private JwtUtil jwtUtil;

        @Test
        void swaggerUiIsDeniedInProd() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void apiDocsAreDeniedInProd() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isForbidden());
        }
    }

    @WebMvcTest
    @ContextConfiguration(classes = OpenApiSecurityTest.DevProfileTest.DevMvcTestConfig.class)
    @AutoConfigureMockMvc(addFilters = true)
    static class DevProfileTest {

        @SpringBootConfiguration
        @EnableAutoConfiguration
        @Import({SecurityConfig.class, JwtFilter.class})
        static class DevMvcTestConfig {
        }

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private JwtUtil jwtUtil;

        @Test
        void swaggerUiIsPublicInDev() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void apiDocsArePublicInDev() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isNotFound());
        }
    }
}
