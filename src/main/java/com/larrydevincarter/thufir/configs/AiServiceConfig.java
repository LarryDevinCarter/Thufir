package com.larrydevincarter.thufir.configs;

import com.larrydevincarter.thufir.services.Assistant;
import com.larrydevincarter.thufir.tools.Tools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceConfig {

    @Bean
    public Assistant assistant(ChatModel chatModel, Tools tools) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(tools)
                .build();
    }
}