package com.logsense.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class AiClientConfig {
    @Bean
    ChatClient chatClient(@Value("${logsense.ai-provider:ollama}") String provider, org.springframework.beans.factory.ObjectProvider<org.springframework.ai.openai.OpenAiChatModel> openai, org.springframework.beans.factory.ObjectProvider<org.springframework.ai.ollama.OllamaChatModel> ollama) {
        ChatModel model = "openai".equalsIgnoreCase(provider) ? openai.getIfAvailable() : ollama.getIfAvailable();
        if (model == null) throw new IllegalStateException("Selected AI model is not configured");
        return ChatClient.builder(model).build();
    }
}
