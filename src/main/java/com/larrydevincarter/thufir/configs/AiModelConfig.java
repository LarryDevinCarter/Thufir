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

    @Value("${xai.temperature:0.7}")
    private Double temperature;

    @Value("${xai.max-tokens:4096}")
    private Integer maxTokens;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
}