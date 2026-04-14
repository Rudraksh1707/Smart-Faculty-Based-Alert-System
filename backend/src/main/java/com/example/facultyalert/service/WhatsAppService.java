package com.example.facultyalert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.number}")
    private String fromNumber;

    @Value("${twilio.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends a WhatsApp message using Twilio's REST API.
     * @param to The recipient's mobile number (should include country code, e.g., +919876543210)
     * @param message The message content
     */
    public void sendMessage(String to, String message) {
        if (!enabled) {
            System.out.println("[SIMULATION] WhatsApp Alert to " + to + ": " + message);
            return;
        }

        if (to == null || to.isEmpty() || accountSid == null || accountSid.contains("xxx")) {
            System.err.println("WhatsApp failed: Recipient number or Twilio credentials missing.");
            return;
        }

        try {
            // Twilio requires the number to be prefixed with 'whatsapp:'
            String formattedTo = to.startsWith("whatsapp:") ? to : "whatsapp:" + to;
            
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            // Set up Basic Auth
            String auth = accountSid + ":" + authToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", fromNumber);
            body.add("To", formattedTo);
            body.add("Body", message);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, request, String.class);
            System.out.println("WhatsApp alert sent successfully to: " + to);
            
        } catch (Exception e) {
            System.err.println("Failed to send WhatsApp message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
