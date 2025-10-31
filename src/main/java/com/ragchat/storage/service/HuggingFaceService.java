package com.ragchat.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HuggingFaceService {

    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceService.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    private final RestTemplate restTemplate;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    @Value("${huggingface.api.model}")
    private String model;

    @Value("${huggingface.api.key}")
    private String apiKey;

    public HuggingFaceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Generate AI response using Hugging Face Inference Providers (OpenAI-Compatible Format)
     * This uses the v1/chat/completions endpoint which is compatible with OpenAI format
     */
    public Map<String, String> generateResponse(String userMessage, String context) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                // Prepare headers
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("Content-Type", "application/json");

                // Build messages array in OpenAI format
                List<Map<String, String>> messages = buildMessages(userMessage, context);

                // Prepare request body (OpenAI-compatible format)
                Map<String, Object> requestBody = buildOpenAIRequest(messages);

                // Create HTTP entity
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                logger.info("Sending request to Hugging Face Inference Providers (attempt {}): {}",
                        attempt + 1, apiUrl);
                logger.debug("Model: {}", model);
                logger.debug("Request body: {}", requestBody);

                // Make API call - Response is in OpenAI format
                ResponseEntity<Map> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                // Parse OpenAI-format response
                return parseOpenAIResponse(response, context);

            } catch (HttpServerErrorException e) {
                if ((e.getStatusCode().value() == 503 || e.getStatusCode().value() == 500)
                        && attempt < MAX_RETRIES - 1) {
                    logger.warn("Server error ({}), waiting {} seconds before retry {}...",
                            e.getStatusCode().value(), RETRY_DELAY_MS / 1000, attempt + 2);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return createErrorResponse("Request interrupted", context);
                    }
                    attempt++;
                    continue;
                }

                logger.error("Server error calling Hugging Face API: {} - {}",
                        e.getStatusCode(), e.getResponseBodyAsString());
                return createErrorResponse("AI service temporarily unavailable. Please try again.", context);

            } catch (HttpClientErrorException e) {
                logger.error("Client error calling Hugging Face API: {} - {}",
                        e.getStatusCode(), e.getResponseBodyAsString());

                if (e.getStatusCode().value() == 401) {
                    return createErrorResponse("Invalid API key. Please check your Hugging Face token.", context);
                } else if (e.getStatusCode().value() == 429) {
                    return createErrorResponse("Rate limit exceeded. Please try again later.", context);
                } else if (e.getStatusCode().value() == 404) {
                    return createErrorResponse("Model not found or endpoint incorrect. Please check configuration.", context);
                }

                return createErrorResponse("Error communicating with AI service", context);

            } catch (Exception e) {
                logger.error("Unexpected error calling Hugging Face API", e);
                return createErrorResponse("Error processing your request: " + e.getMessage(), context);
            }
        }

        return createErrorResponse("Failed to get response after retries. Please try again.", context);
    }

    /**
     * Build messages array in OpenAI format
     * Format: [{"role": "system", "content": "..."}, {"role": "user", "content": "..."}]
     */
    private List<Map<String, String>> buildMessages(String userMessage, String context) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System message
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful AI assistant. Provide clear, accurate, and concise responses.");
        messages.add(systemMessage);

        // Add context if available (as previous conversation)
        if (context != null && !context.isEmpty()) {
            // Simple context handling - you can make this more sophisticated
            Map<String, String> contextMsg = new HashMap<>();
            contextMsg.put("role", "assistant");
            contextMsg.put("content", context);
            messages.add(contextMsg);
        }

        // Current user message
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        return messages;
    }

    /**
     * Build request body in OpenAI-compatible format
     */
    private Map<String, Object> buildOpenAIRequest(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 512);
        requestBody.put("temperature", 0.7);
        requestBody.put("top_p", 0.9);
        requestBody.put("stream", false);

        return requestBody;
    }

    /**
     * Parse OpenAI-format response
     * Expected format:
     * {
     *   "choices": [{
     *     "message": {
     *       "role": "assistant",
     *       "content": "response text"
     *     }
     *   }]
     * }
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseOpenAIResponse(ResponseEntity<Map> response, String context) {
        Map<String, Object> responseBody = response.getBody();

        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

                if (message != null && message.containsKey("content")) {
                    String generatedText = (String) message.get("content");

                    logger.info("Successfully received response from Hugging Face");
                    logger.debug("Generated text: {}", generatedText);

                    if (generatedText != null && !generatedText.trim().isEmpty()) {
                        Map<String, String> result = new HashMap<>();
                        result.put("content", generatedText.trim());
                        result.put("context", buildContextString(context, generatedText));
                        return result;
                    }
                }
            }
        }

        logger.warn("Empty or invalid response from Hugging Face API");
        logger.debug("Response body: {}", responseBody);
        return createDefaultResponse(context);
    }

    /**
     * Build context string for next message
     */
    private String buildContextString(String oldContext, String newResponse) {
        if (oldContext != null && !oldContext.isEmpty()) {
            return oldContext + "\n" + newResponse;
        }
        return newResponse;
    }

    private Map<String, String> createDefaultResponse(String context) {
        Map<String, String> result = new HashMap<>();
        result.put("content", "I'm here to help! Could you please rephrase your question?");
        result.put("context", context != null ? context : "");
        return result;
    }

    private Map<String, String> createErrorResponse(String errorMessage, String context) {
        Map<String, String> result = new HashMap<>();
        result.put("content", errorMessage);
        result.put("context", context != null ? context : "");
        return result;
    }
}