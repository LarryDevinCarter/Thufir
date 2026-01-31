package com.larrydevincarter.thufir.controllers;

import com.larrydevincarter.thufir.services.Assistant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thufir")
public class ThufirController {

    private final Assistant chattingAssistant;
    private final Assistant workingAssistant;

    public ThufirController(@Qualifier("chattingAssistant") Assistant chattingAssistant,
                            @Qualifier("workingAssistant") Assistant workingAssistant) {
        this.chattingAssistant = chattingAssistant;
        this.workingAssistant = workingAssistant;
    }

    @PostMapping("/chat")
    public String casualChat(@RequestBody String message) {
        return chattingAssistant.chat(message);
    }

    @PostMapping("/work")
    public String workMode(@RequestBody String message) {
        return workingAssistant.chat(message);
    }
}