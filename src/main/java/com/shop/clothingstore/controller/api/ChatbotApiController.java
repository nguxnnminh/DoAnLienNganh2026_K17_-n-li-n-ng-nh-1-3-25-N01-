package com.shop.clothingstore.controller.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.service.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    public ChatbotApiController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // POST /api/chatbot
    // Body: { "message": "tôi muốn áo màu đen dưới 500k" }
    @PostMapping
    public ResponseEntity<ChatbotService.ChatbotResponse> chat(
            @RequestBody Map<String, String> request) {

        String message = request.getOrDefault("message", "");

        ChatbotService.ChatbotResponse response = chatbotService.processMessage(message);

        return ResponseEntity.ok(response);
    }
}
