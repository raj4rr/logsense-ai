package com.logsense.api;

import com.logsense.mcp.client.McpChatClientService;
import com.logsense.mcp.client.McpChatClientService.PromptSuggestion;
import com.logsense.mcp.client.McpChatClientService.ToolInfo;
import com.logsense.mcp.client.McpChatClientService.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatMcpController {

    private final McpChatClientService chatService;

    public ChatMcpController(McpChatClientService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/prompts")
    public List<PromptSuggestion> getPrompts() {
        return chatService.getPromptSuggestions();
    }

    @GetMapping("/tools")
    public List<ToolInfo> getTools() {
        return chatService.getAvailableTools();
    }

    @PostMapping("/message")
    public ChatResponse sendMessage(@RequestBody Map<String, String> payload) {
        String message = payload.getOrDefault("message", "");
        return chatService.processChat(message);
    }
}
