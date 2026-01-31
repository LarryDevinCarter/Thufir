package com.larrydevincarter.thufir.configs;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    @Value("${xai.api.key}")
    private String apiKey;

    @Value("${xai.base.url}")
    private String baseUrl;

    @Value("${xai.model.name}")
    private String modelName;

    @Value("${xai.max-tokens:4096}")
    private Integer maxTokens;

    @Bean
    public ChatModel chatModelHighTemp() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(maxTokens)
                .build();
    }

    @Bean
    public ChatModel chatModelLowTemp() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.4)
                .maxTokens(maxTokens)
                .build();
    }
}