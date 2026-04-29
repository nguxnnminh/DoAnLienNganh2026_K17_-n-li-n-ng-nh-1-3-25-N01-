package com.shop.clothingstore.controller.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.shop.clothingstore.security.JwtUtil;
import com.shop.clothingstore.service.CartService;
import com.shop.clothingstore.service.AiChatbotService;
import com.shop.clothingstore.dto.api.ChatbotResponse;
import com.shop.clothingstore.service.CustomUserDetailsService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ChatbotApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiChatbotService chatbotService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // =============================================================
    // POST /api/chatbot - Normal cases
    // =============================================================
    @Test
    @DisplayName("POST /api/chatbot with greeting returns 200 and message")
    void chat_Greeting_ReturnsOkWithMessage() throws Exception {
        ChatbotResponse response = ChatbotResponse.text("Xin chào! Tôi là trợ lý mua sắm.");
        when(chatbotService.isEnabledAndConfigured()).thenReturn(true);
        when(chatbotService.processMessage("xin chào")).thenReturn(response);

        mockMvc.perform(post("/api/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"xin chào\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xin chào! Tôi là trợ lý mua sắm."))
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products").isEmpty());
    }

    @Test
    @DisplayName("POST /api/chatbot with product search returns products list")
    void chat_ProductSearch_ReturnsOkWithProducts() throws Exception {
        ChatbotResponse response = ChatbotResponse.text("Tìm thấy 2 sản phẩm:");
        when(chatbotService.isEnabledAndConfigured()).thenReturn(true);
        when(chatbotService.processMessage("áo thun")).thenReturn(response);

        mockMvc.perform(post("/api/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"áo thun\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tìm thấy 2 sản phẩm:"));
    }

    @Test
    @DisplayName("POST /api/chatbot with empty body returns welcome response")
    void chat_EmptyBody_ReturnsWelcome() throws Exception {
        ChatbotResponse response = ChatbotResponse.text("Xin chào!");
        when(chatbotService.isEnabledAndConfigured()).thenReturn(true);
        when(chatbotService.processMessage("")).thenReturn(response);

        mockMvc.perform(post("/api/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xin chào!"));
    }

    @Test
    @DisplayName("POST /api/chatbot without message field defaults to empty string")
    void chat_NoMessageField_ReturnsOk() throws Exception {
        ChatbotResponse response = ChatbotResponse.text("Xin chào!");
        when(chatbotService.isEnabledAndConfigured()).thenReturn(true);
        when(chatbotService.processMessage("")).thenReturn(response);

        mockMvc.perform(post("/api/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xin chào!"));
    }

    // =============================================================
    // Verifies the API is public (no auth required)
    // =============================================================
    @Test
    @DisplayName("POST /api/chatbot is accessible without authentication")
    void chat_PublicEndpoint_NoAuthRequired() throws Exception {
        when(chatbotService.isEnabledAndConfigured()).thenReturn(true);
        when(chatbotService.processMessage(anyString()))
                .thenReturn(ChatbotResponse.text("OK"));

        mockMvc.perform(post("/api/chatbot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"test\"}"))
                .andExpect(status().isOk());
    }
}
