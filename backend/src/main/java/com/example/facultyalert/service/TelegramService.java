package com.example.facultyalert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String chatId, String text) {
        if (chatId == null || chatId.isEmpty() || botToken == null || botToken.isEmpty()) return;

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            
            Map<String, String> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            restTemplate.postForEntity(url, body, String.class);
            System.out.println("Telegram sent successfully to ChatID: " + chatId);
        } catch (Exception e) {
            System.err.println("Failed to send Telegram message: " + e.getMessage());
        }
    }
}
