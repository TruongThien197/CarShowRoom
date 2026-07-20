package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.repository.CarModelRepository;
import com.hsf302.carshowroom.service.CategoryService;
import com.hsf302.carshowroom.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CarModelRepository carModelRepository;

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer carModelId,
                       @RequestParam(required = false) String brand,
                       @RequestParam(required = false) String modelName,
                       @RequestParam(required = false) Integer year,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "9") int size,
                       Model model) {
        Page<Product> productPage = productService.findProductsPaged(categoryId, keyword, carModelId, brand, modelName, year, PageRequest.of(page, size));
        populateCatalogModel(model, productPage, categoryId, keyword, carModelId, brand, modelName, year, page);
        return "products/list";
    }

    @GetMapping("/products")
    public String listProducts(@RequestParam(required = false) Integer categoryId,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer carModelId,
                               @RequestParam(required = false) String brand,
                               @RequestParam(required = false) String modelName,
                               @RequestParam(required = false) Integer year,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "9") int size,
                               Model model) {
        Page<Product> productPage = productService.findProductsPaged(categoryId, keyword, carModelId, brand, modelName, year, PageRequest.of(page, size));
        populateCatalogModel(model, productPage, categoryId, keyword, carModelId, brand, modelName, year, page);
        return "products/list";
    }

    @GetMapping("/products/category/{categoryId}")
    public String filterByCategory(@PathVariable Integer categoryId,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer carModelId,
                                   @RequestParam(required = false) String brand,
                                   @RequestParam(required = false) String modelName,
                                   @RequestParam(required = false) Integer year,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "9") int size,
                                   Model model) {
        Page<Product> productPage = productService.findProductsPaged(categoryId, keyword, carModelId, brand, modelName, year, PageRequest.of(page, size));
        populateCatalogModel(model, productPage, categoryId, keyword, carModelId, brand, modelName, year, page);
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productService.getProduct(id));
        return "products/detail";
    }

    private void populateCatalogModel(Model model, Page<Product> productPage, Integer categoryId, String keyword,
                                      Integer carModelId, String brand, String modelName, Integer year, int page) {
        List<Category> categories = categoryService.getAllCategories();
        String selectedCategoryName = categories.stream()
                .filter(category -> category.getId().equals(categoryId))
                .map(Category::getCategoryName)
                .findFirst()
                .orElse("");

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("carModels", carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc());
        model.addAttribute("visibleCarModels", (brand == null || brand.isBlank())
                ? List.of()
                : carModelRepository.findByBrandOrderByModelNameAscYearDesc(brand));
        model.addAttribute("carBrands", carModelRepository.findDistinctBrands());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedCategoryName", selectedCategoryName);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCarModelId", carModelId);
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("selectedModelName", modelName);
        model.addAttribute("selectedYear", year);
        model.addAttribute("currentPage", page);
        int totalPages = productPage.getTotalPages();
        int paginationStart = Math.max(0, page - 2);
        int paginationEnd = Math.min(totalPages - 1, paginationStart + 4);
        paginationStart = Math.max(0, paginationEnd - 4);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("paginationStart", paginationStart);
        model.addAttribute("paginationEnd", paginationEnd);
    }
}
