package com.larrydevincarter.thufir.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class Tools {

    @Tool("Get the current date and time in ISO format")
    public String currentDateTime() {
        return LocalDateTime.now().toString();
    }
}