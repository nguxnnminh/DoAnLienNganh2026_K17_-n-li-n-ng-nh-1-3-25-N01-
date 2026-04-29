package com.shop.clothingstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chatbot.ai")
public class ChatbotAiProperties {

    /**
     * Master switch for AI chatbot.
     * If disabled, the app will always use the rule-based chatbot.
     */
    private boolean enabled = false;

    /**
     * Ollama base URL, e.g. http://localhost:11434
     */
    private String ollamaBaseUrl = "http://localhost:11434";

    /**
     * Ollama model name, e.g. llama3.1
     */
    private String ollamaModel = "llama3.1";

    /**
     * Request timeout in milliseconds.
     */
    private int timeoutMs = 10_000;

    /**
     * If OpenAI returns a rate limit / quota error, temporarily disable AI calls
     * to avoid spamming the API and logs.
     */
    private int cooldownSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}

