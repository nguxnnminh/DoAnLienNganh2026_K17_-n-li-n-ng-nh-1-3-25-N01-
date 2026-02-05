package com.shop.clothingstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.specification.ProductSpecification;

@Controller
public class ShopController {

    private final ProductRepository productRepository;

    public ShopController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // HOME / SHOP
    @GetMapping("/")
    public String home(ProductFilterDTO filter, Model model) {

        var products = productRepository.findAll(
                ProductSpecification.filter(filter)
        );

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        return "shop/home";
    }

    // PRODUCT DETAIL
    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {

        var product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        model.addAttribute("product", product);
        return "shop/product-detail";
    }
}
