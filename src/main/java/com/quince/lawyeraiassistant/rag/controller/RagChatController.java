package com.quince.lawyeraiassistant.rag.controller;

import com.quince.lawyeraiassistant.rag.config.ConditionalOnRagEnabled;
import com.quince.lawyeraiassistant.rag.service.RagChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnRagEnabled
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    @GetMapping("/chat")
    public String chat(
            @RequestParam("question") String question) {

        return ragChatService.chat(question);
    }

}
