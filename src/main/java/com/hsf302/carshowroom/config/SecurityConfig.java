package com.hsf302.carshowroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth ->
                    auth.requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                            .requestMatchers("/staff", "/staff/**").hasRole("STAFF")
                            .requestMatchers("/cart", "/cart/**").hasRole("CUSTOMER")
                            .requestMatchers("/orders", "/orders/**").hasRole("CUSTOMER")
                            .requestMatchers("/booking", "/booking/**").hasRole("CUSTOMER")
                            .requestMatchers("/vehicles", "/vehicles/**").hasRole("CUSTOMER")
                            .requestMatchers("/account", "/account/**").hasRole("CUSTOMER")
                            .requestMatchers("/", "/shop", "/products/**", "/auth/**", "/bootstrap.css",
                                    "/bootstrap.js", "/bootstrap-icons.css", "/apex.css", "/fonts/**", "/images/**")
                            .permitAll()
                            .anyRequest().permitAll())
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendRedirect("/auth/login"))
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        if (request.isUserInRole("ADMIN")) {
                            response.sendRedirect("/admin");
                        } else if (request.isUserInRole("STAFF")) {
                            response.sendRedirect("/staff");
                        } else if (request.isUserInRole("CUSTOMER")) {
                            response.sendRedirect("/");
                        } else {
                            response.sendRedirect("/auth/login");
                        }
                    }))
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable());
        return http.build();
    }
}
