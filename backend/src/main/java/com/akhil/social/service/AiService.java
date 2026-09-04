package com.akhil.social.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.base-url}")
    private String baseUrl;
    @Value("${ai.api-key:}")
    private String apiKey;
    @Value("${ai.model}")
    private String model;

    public Map<String, Object> chat(String prompt, String context) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(prompt, "AI_API_KEY not configured");
        }
        try {
            String system = "You are NEXUS AI Agent, a helpful social intelligence assistant for a unified WhatsApp/Instagram/Facebook/X workspace. " +
                    "Provide concise, professional, actionable responses. Never execute destructive actions; only suggest and prepare drafts for user confirmation. " +
                    "Workspace context (JSON): " + (context != null ? context : "{}");

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 1024
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl.replaceAll("/$", "") + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return fallback(prompt, "AI provider returned " + resp.getStatusCode());
            }

            JsonNode root = mapper.readTree(resp.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return fallback(prompt, "Empty AI response");
            }
            return Map.of(
                    "success", true,
                    "source", "ai",
                    "provider", baseUrl.contains("groq") ? "groq" : "openai-compatible",
                    "model", model,
                    "reply", content
            );
        } catch (RestClientException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "network error";
            if (msg.contains("401") || msg.contains("403")) return fallback(prompt, "AI authentication failed");
            if (msg.contains("429")) return fallback(prompt, "AI rate limit exceeded");
            return fallback(prompt, "AI unavailable: " + msg);
        } catch (Exception e) {
            return fallback(prompt, "AI processing error");
        }
    }

    private Map<String, Object> fallback(String prompt, String reason) {
        String lower = prompt.toLowerCase();
        String reply;
        if (lower.contains("summar") || lower.contains("summary")) {
            reply = "[Local fallback] Workspace summary: Review recent messages and posts in the sidebar. Engage with high-activity conversations first.";
        } else if (lower.contains("reply") || lower.contains("draft") || lower.contains("message")) {
            reply = "[Local fallback] Suggested reply: Thanks for the update — I'll review and get back to you shortly.";
        } else if (lower.contains("caption") || lower.contains("post idea")) {
            reply = "[Local fallback] Caption idea: Building in public with NEXUS — one workspace for every conversation that matters.";
        } else {
            reply = "[Local fallback] AI is temporarily unavailable (" + reason + "). Try again later or refine your prompt.";
        }
        return Map.of(
                "success", true,
                "source", "fallback",
                "provider", "local",
                "model", "none",
                "reply", reply,
                "reason", reason
        );
    }
}
