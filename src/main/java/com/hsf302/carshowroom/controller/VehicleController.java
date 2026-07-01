package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.dto.VehicleForm;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.CarModelService;
import com.hsf302.carshowroom.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final AuthService authService;
    private final VehicleService vehicleService;
    private final CarModelService carModelService;

    @GetMapping
    public String list(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("vehicles", vehicleService.getVehicles(user));
        return "vehicles/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        populateFormModel(model, new VehicleForm());
        return "vehicles/add";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("vehicleForm") VehicleForm form,
                      BindingResult bindingResult,
                      Model model) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form);
            return "vehicles/add";
        }
        vehicleService.addVehicle(user, form);
        return "redirect:/vehicles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id) {
        User user = currentUserOrNull();
        if (user == null) {
            return "redirect:/auth/login";
        }
        vehicleService.deleteVehicle(user, id);
        return "redirect:/vehicles";
    }

    private void populateFormModel(Model model, VehicleForm form) {
        model.addAttribute("vehicleForm", form);
        model.addAttribute("carModels", carModelService.getAllCarModels());
    }

    private User currentUserOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
