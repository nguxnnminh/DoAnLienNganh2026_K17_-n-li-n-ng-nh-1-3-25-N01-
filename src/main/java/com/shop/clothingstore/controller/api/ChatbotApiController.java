package com.shop.clothingstore.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.clothingstore.service.AiChatbotService;
import com.shop.clothingstore.dto.api.ChatbotResponse;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotApiController {

    private static final String HISTORY_KEY = "chatbot_history";
    // Keep at most this many entries (user + assistant counted together) in the session.
    private static final int MAX_HISTORY = 12;

    private final AiChatbotService chatbotService;

    public ChatbotApiController(AiChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // POST /api/chatbot
    // Body: { "message": "I'm looking for a black shirt under 500k" }
    @PostMapping
    public ResponseEntity<ChatbotResponse> chat(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        String message = request.getOrDefault("message", "");

        if (!chatbotService.isEnabledAndConfigured()) {
            return ResponseEntity.ok(ChatbotResponse.text(
                    "Chatbot AI is not enabled. Please start Ollama and set CHATBOT_AI_ENABLED=true, then restart the backend."
            ));
        }

        List<Map<String, Object>> history = getHistory(session);

        ChatbotResponse response = chatbotService.processMessage(message, history);

        // Persist this turn so follow-up questions retain context within the session
        if (message != null && !message.isBlank()) {
            history.add(Map.of("role", "user", "content", message));
            if (response.getMessage() != null && !response.getMessage().isBlank()) {
                history.add(Map.of("role", "assistant", "content", response.getMessage()));
            }
            trim(history);
            session.setAttribute(HISTORY_KEY, history);
        }

        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getHistory(HttpSession session) {
        Object stored = session.getAttribute(HISTORY_KEY);
        if (stored instanceof List<?> list) {
            return new ArrayList<>((List<Map<String, Object>>) list);
        }
        return new ArrayList<>();
    }

    private void trim(List<Map<String, Object>> history) {
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }
}
