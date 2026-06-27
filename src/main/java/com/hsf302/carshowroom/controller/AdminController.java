package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private UserService userService;

    @GetMapping
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user-list";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("user", userService.getUserByid(id));
        return "admin/user-detail";
    }

    @GetMapping("/users/create")
    public String createUserForm() {
        return "admin/user-create";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam("txtEmail") String email,
                             @RequestParam("txtPassword") String password,
                             @RequestParam("txtFullName") String fullName,
                             @RequestParam(value = "txtPhone", required = false) String phone,
                             @RequestParam(value = "txtAddress", required = false) String address,
                             @RequestParam(defaultValue = "CUSTOMER") String role) {
        userService.createUser(email, password, fullName, phone, address, role);
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Integer id, Model model) {
        model.addAttribute("user", userService.getUserByid(id));
        return "admin/user-edit";
    }

    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Integer id,
                           @RequestParam("txtFullName") String fullName,
                           @RequestParam(value = "txtPhone", required = false) String phone,
                           @RequestParam(value = "txtAddress", required = false) String address,
                           @RequestParam(defaultValue = "CUSTOMER") String role) {
        userService.updateUser(id, fullName, phone, address, role);
        return "redirect:/admin/users";
    }

    @GetMapping("users/{id}/change-status")
    public String changeStatus(@PathVariable Integer id) {
        userService.changeStatus(id);
        return "redirect:/admin/users";
    }
}
