package com.larrydevincarter.thufir.controllers;

import com.larrydevincarter.thufir.services.Assistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thufir")
public class ThufirController {

    private final Assistant assistant;

    public ThufirController(Assistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return assistant.chat(message);
    }
}