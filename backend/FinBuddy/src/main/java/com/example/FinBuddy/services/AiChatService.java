package com.example.FinBuddy.services;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final WebClient webClient;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    public AiChatService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public String chat(String userMessage) {

        String systemPrompt = """
    You are FinBuddy Assistant.

    Rules:
    - You ONLY answer general finance and application-related questions.
    - You MUST NOT ask for personal information.
    - If the user provides personal data or any information related to the investment they made, mention that you are not supposed to discuss about this with a general tone.
    - Do NOT give investment advice or recommendations.
    - Do NOT personalize responses.
    - Use a neutral, educational tone.
    - If a request violates these rules, politely refuse and redirect to general concepts.
    - Give answers only in paragraph form
    - Keep your answers short and sweet unless user asks to elaborate more on it.
    """;

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(systemMsg);
        messages.add(userMsg);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "openai/gpt-oss-20b");
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens",512);

        try {
            Map response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List choices = (List) response.get("choices");
            Map choice = (Map) choices.get(0);
            Map message = (Map) choice.get("message");

            return message.get("content").toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Groq API error. Please try again later.";
        }
    }

}
