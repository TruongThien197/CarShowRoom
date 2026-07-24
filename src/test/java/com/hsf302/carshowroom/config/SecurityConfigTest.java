package com.hsf302.carshowroom.config;

import com.hsf302.carshowroom.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigTest.AdminProbeController.class)
class SecurityConfigTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/probe"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?authRequired=true"));
    }

    @Test
    void customerCannotAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/probe").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void staffCannotAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/probe").with(user("staff").roles("STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff"));
    }

    @Test
    void adminCanAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/probe").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Controller
    @RequestMapping("/admin")
    static class AdminProbeController {
        @GetMapping("/probe")
        @ResponseBody
        String probe() {
            return "probe";
        }
    }
}
