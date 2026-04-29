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
    // Body: { "message": "tôi muốn áo màu đen dưới 500k" }
    @PostMapping
    public ResponseEntity<ChatbotResponse> chat(
            @RequestBody Map<String, String> request) {

        String message = request.getOrDefault("message", "");

        if (!chatbotService.isEnabledAndConfigured()) {
            return ResponseEntity.ok(ChatbotResponse.text(
                    "Chatbot AI chưa được bật/cấu hình. Bạn hãy bật Ollama và set CHATBOT_AI_ENABLED=true rồi restart backend."
            ));
        }

        ChatbotResponse response = chatbotService.processMessage(message);

        return ResponseEntity.ok(response);
    }
}
