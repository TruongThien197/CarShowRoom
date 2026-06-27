package com.hsf302.carshowroom.config;

import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final AuthService authService;

    @ModelAttribute("currentUser")
    public User currentUser() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
