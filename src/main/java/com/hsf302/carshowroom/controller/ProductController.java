package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.repository.CategoryRepository;
import com.hsf302.carshowroom.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        model.addAttribute("products", productService.findProducts(categoryId, keyword));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        return "shop/index";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productService.getProduct(id));
        return "shop/detail";
    }
}
