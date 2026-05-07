package com.shop.clothingstore.controller.admin;

import org.springframework.data.domain.Page;
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

    /**
     * Adds ±2 pagination window attributes so templates never render
     * more than 7 page buttons regardless of total page count.
     *
     * Adds to model:
     *   pageWindowStart      — first page number in the window
     *   pageWindowEnd        — last  page number in the window
     *   showStartEllipsis    — true when there are pages before the window
     *   showEndEllipsis      — true when there are pages after the window
     */
    protected void addPageWindow(Model model, Page<?> page) {
        int current     = page.getNumber();
        int total       = page.getTotalPages();
        int windowStart = Math.max(0, current - 2);
        int windowEnd   = Math.min(total - 1, current + 2);
        model.addAttribute("pageWindowStart",   windowStart);
        model.addAttribute("pageWindowEnd",     windowEnd);
        model.addAttribute("showStartEllipsis", windowStart > 1);
        model.addAttribute("showEndEllipsis",   windowEnd < total - 2);
    }
}