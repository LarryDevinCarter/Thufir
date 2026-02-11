package com.larrydevincarter.thufir.configs;

import com.larrydevincarter.thufir.services.Assistant;
import com.larrydevincarter.thufir.tools.*;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Configuration
public class AiServiceConfig {

    @Bean
    public ChatMemory sharedChatMemory() {

        String soul;

        try {

            ClassPathResource resource = new ClassPathResource("thufir-soul.md");
            soul = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        } catch (IOException e) {

            throw new RuntimeException("Failed to load Thufir's soul from thufir-soul.md. " +
                    "Ensure the file exists in src/main/resources/. Error: " + e.getMessage(), e);

        }

        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(50);
        memory.add(SystemMessage.from(soul));
        return memory;
    }



    @Bean
    public Assistant chattingAssistant(ChatModel chatModelHighTemp, Tools tools, CommunicationTools communicationTools) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModelHighTemp)
                .chatMemory(sharedChatMemory())
                .tools(tools, communicationTools)
                .build();
    }

    @Bean
    public Assistant workingAssistant(ChatModel chatModelLowTemp, Tools tools, CommunicationTools communicationTools) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModelLowTemp)
                .chatMemory(sharedChatMemory())
                .tools(tools, communicationTools)
                .build();
    }
}