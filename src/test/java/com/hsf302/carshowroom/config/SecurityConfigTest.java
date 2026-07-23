package com.hsf302.carshowroom.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.AdminProbeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/probe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?authRequired=true"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/probe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/probe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/probe"))
                .andExpect(status().isOk());
    }

    @Controller
    @RequestMapping("/admin")
    static class AdminProbeController {
        @GetMapping("/probe")
        String probe() {
            return "probe";
        }
    }
}
