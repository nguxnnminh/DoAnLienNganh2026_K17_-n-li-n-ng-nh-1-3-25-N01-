package com.shop.clothingstore.controller.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.service.AiChatbotService;
import com.shop.clothingstore.dto.api.ChatbotResponse;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private final AiChatbotService chatbotService;

    public ChatbotApiController(AiChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // POST /api/chatbot
    // Body: { "message": "I'm looking for a black shirt under 500k" }
    @PostMapping
    public ResponseEntity<ChatbotResponse> chat(
            @RequestBody Map<String, String> request) {

        String message = request.getOrDefault("message", "");

        if (!chatbotService.isEnabledAndConfigured()) {
            return ResponseEntity.ok(ChatbotResponse.text(
                    "Chatbot AI is not enabled. Please start Ollama and set CHATBOT_AI_ENABLED=true, then restart the backend."
            ));
        }

        ChatbotResponse response = chatbotService.processMessage(message);

        return ResponseEntity.ok(response);
    }
}
