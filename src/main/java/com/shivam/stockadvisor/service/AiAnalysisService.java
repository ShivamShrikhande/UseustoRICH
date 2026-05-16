package com.shivam.stockadvisor.service;

import com.shivam.stockadvisor.model.Stock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * AI Analysis Service.
 * If OPENAI_API_KEY is set → calls OpenAI GPT-3.5 directly via RestTemplate.
 * If not set or blocked → uses smart local template engine (works offline, zero config).
 */
@Service
public class AiAnalysisService {

    // Reads from application.properties or environment variable
    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Main entry point. Tries real OpenAI first, falls back to local engine.
     */
    public void generateExplanation(Stock stock) {
        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            try {
                String aiText = callOpenAiDirectly(stock);
                stock.setAiExplanation(aiText);
                System.out.println("🤖 OpenAI insight generated for " + stock.getSymbol());
                return;
            } catch (Exception e) {
                System.out.println("⚠️ OpenAI API failed (" + e.getMessage() + "), using local engine");
            }
        }

        // Fallback: local template engine (deterministic, instant, free, works offline)
        stock.setAiExplanation(generateLocalExplanation(stock));
        System.out.println("🧠 Local AI insight generated for " + stock.getSymbol());
    }

    // ========== REAL OPENAI (Direct HTTP call, no Spring AI dependency) ==========
    private String callOpenAiDirectly(Stock stock) throws Exception {
        String prompt = String.format(
                "You are a senior technical analyst. Analyze %s (%s) stock concisely in exactly 2 sentences.\n\n" +
                        "Data:\n" +
                        "- Price: $%.2f (%.2f%% today)\n" +
                        "- RSI(14): %.1f (overbought >70, oversold <30)\n" +
                        "- SMA20: $%.2f, SMA50: $%.2f\n" +
                        "- MACD Histogram: %+.2f\n" +
                        "- Computed Signal: %s\n\n" +
                        "Give specific, actionable insight about momentum and trend. No generic advice.",
                stock.getSymbol(), stock.getName(),
                stock.getCurrentPrice(), stock.getChangePercent(),
                stock.getRsi14(), stock.getSma20(), stock.getSma50(),
                stock.getMacdHistogram(), stock.getRecommendation()
        );

        // Build request body
        String requestBody = String.format(
                "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":0.7,\"max_tokens\":150}",
                prompt.replace("\"", "\\\"").replace("\n", "\\n")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openAiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Parse JSON response manually
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText().trim();
    }

    // ========== LOCAL AI (works without internet/API key) ==========
    private String generateLocalExplanation(Stock stock) {
        String rec = stock.getRecommendation();
        double rsi = stock.getRsi14();
        double macd = stock.getMacdHistogram();
        String symbol = stock.getSymbol();

        int variant = Math.abs(symbol.hashCode()) % 2;

        if ("BUY".equals(rec)) {
            if (variant == 0) {
                return String.format(
                        "%s is flashing bullish signals with RSI at %.1f and positive MACD momentum at %+.2f. " +
                                "The golden cross structure suggests accumulating on any dips toward the SMA50 support.",
                        symbol, rsi, macd
                );
            } else {
                return String.format(
                        "Technical picture for %s is strengthening — RSI at %.1f leaves room before overbought territory, " +
                                "while MACD histogram at %+.2f confirms buyers are firmly in control of the trend.",
                        symbol, rsi, macd
                );
            }
        } else if ("SELL".equals(rec)) {
            if (variant == 0) {
                return String.format(
                        "%s is under distribution pressure with bearish MACD at %+.2f and RSI weakening toward %.1f. " +
                                "Consider reducing exposure until price reclaims the SMA20 dynamically.",
                        symbol, macd, rsi
                );
            } else {
                return String.format(
                        "Momentum is deteriorating for %s — the death cross alignment and negative MACD histogram at %+.2f " +
                                "suggest further downside risk ahead. Defensive positioning is warranted here.",
                        symbol, macd
                );
            }
        } else {
            if (variant == 0) {
                return String.format(
                        "%s is in a consolidation phase with mixed indicator readings. " +
                                "Wait for a clear breakout above SMA20 ($%.2f) or breakdown below SMA50 ($%.2f) before sizing up.",
                        symbol, stock.getSma20(), stock.getSma50()
                );
            } else {
                return String.format(
                        "No strong edge for %s right now — RSI at %.1f is neutral and MACD lacks directional conviction at %+.2f. " +
                                "Patience is the better trade here until the trend resolves.",
                        symbol, rsi, macd
                );
            }
        }
    }
}