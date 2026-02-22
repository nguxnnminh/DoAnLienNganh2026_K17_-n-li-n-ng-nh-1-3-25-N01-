package com.shop.clothingstore.controller.admin;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

public abstract class AdminBaseController {

    @ModelAttribute
    public void globalAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());

        if (!model.containsAttribute("title")) {
            model.addAttribute("title", "Admin");
        }
    }
}