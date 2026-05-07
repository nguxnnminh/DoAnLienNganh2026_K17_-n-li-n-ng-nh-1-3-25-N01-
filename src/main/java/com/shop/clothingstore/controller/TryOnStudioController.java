package com.shop.clothingstore.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.service.TryOnService;

/**
 * Controller for the dedicated Virtual Try-On Studio page. Loads all
 * try-on-enabled products grouped by garment type so users can mix-and-match
 * complete outfits.
 */
@Controller
public class TryOnStudioController {

    private final TryOnService tryOnService;

    public TryOnStudioController(TryOnService tryOnService) {
        this.tryOnService = tryOnService;
    }

    @GetMapping("/tryon-studio")
    public String studio(Model model) {
        List<Product> allTryOn = tryOnService.findAllTryOnEnabled();

        // Group by garment type for the UI tabs
        Map<String, List<Product>> grouped = allTryOn.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getGarmentType() != null ? p.getGarmentType().name() : "OTHER",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("tryOnProducts", allTryOn);
        model.addAttribute("upperProducts", grouped.getOrDefault("UPPER_BODY", List.of()));
        model.addAttribute("lowerProducts", grouped.getOrDefault("LOWER_BODY", List.of()));

        return "shop/tryon-studio";
    }
}
