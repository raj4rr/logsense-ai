package com.logsense.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerAiConfig {

    @Bean
    public ChatClient serverChatClient(
            @Value("${logsense.ai-provider:ollama}") String provider,
            ObjectProvider<org.springframework.ai.openai.OpenAiChatModel> openai,
            ObjectProvider<org.springframework.ai.ollama.OllamaChatModel> ollama) {
        ChatModel model = "openai".equalsIgnoreCase(provider) ? openai.getIfAvailable() : ollama.getIfAvailable();
        if (model == null) {
            model = ollama.getIfAvailable();
        }
        if (model == null) {
            model = openai.getIfAvailable();
        }
        if (model == null) {
            return ChatClient.builder(new NoOpChatModel()).build();
        }
        return ChatClient.builder(model).build();
    }

    private static class NoOpChatModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt prompt) {
            return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of());
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getDefaultOptions() {
            return org.springframework.ai.chat.prompt.ChatOptions.builder().build();
        }
    }
}
