package com.logsense.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientConfig {
    @Bean
    @ConditionalOnProperty(prefix = "logsense.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
    ChatClient ollamaChatClient(OllamaChatModel model) { return ChatClient.builder(model).build(); }

    @Bean
    @ConditionalOnProperty(prefix = "logsense.ai", name = "provider", havingValue = "openai")
    ChatClient openAiChatClient(OpenAiChatModel model) { return ChatClient.builder(model).build(); }
}
