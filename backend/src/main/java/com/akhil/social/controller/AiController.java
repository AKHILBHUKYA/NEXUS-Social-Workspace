package com.akhil.social.controller;

import com.akhil.social.service.AiService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;
    public AiController(AiService aiService) { this.aiService = aiService; }

    public record ChatRequest(@NotBlank String prompt, String context) {}

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest req) {
        return aiService.chat(req.prompt(), req.context());
    }
}
