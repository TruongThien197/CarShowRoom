package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    private SecurityContextRepository scp = new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("txtEmail") String email,
                        @RequestParam("txtPassword") String password,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        User user = authService.login(email, password);
        if (user != null) {
            String role = user.getRole().toUpperCase();
            user.setRole(role);
            var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            scp.saveContext(context, request, response);
            if ("ADMIN".equals(role)) return "redirect:/admin";
            if ("STAFF".equals(role)) return "redirect:/staff";
        }
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }


    @PostMapping("/register")
    public String register(@RequestParam("txtEmail") String email,
                           @RequestParam("txtPassword") String password,
                           @RequestParam("txtfullName") String fullName,
                           @RequestParam(value = "txtPhone", required = false) String phone,
                           @RequestParam(value = "txtAddress", required = false) String address) {
        authService.register(email, password, fullName, phone, address);
        return "redirect:/auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/";
    }
}
