package com.example.FinBuddy.controllers;

import com.example.FinBuddy.dto.ChatRequestDTO;
import com.example.FinBuddy.dto.ChatResponseDTO;
import com.example.FinBuddy.services.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        String reply = aiChatService.chat(request.getMessage());
        return ResponseEntity.ok(new ChatResponseDTO(reply));
    }
}
