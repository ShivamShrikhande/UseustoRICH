package com.shivam.stockadvisor.service;

import com.shivam.stockadvisor.model.Stock;
import com.shivam.stockadvisor.model.StockPricePoint;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YahooFinanceService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Cache for current price (5 min)
    private final Map<String, CachedStock> priceCache = new ConcurrentHashMap<>();
    private static final long PRICE_CACHE_MS = 5 * 60 * 1000;

    // Cache for historical data (1 hour)
    private final Map<String, CachedHistory> historyCache = new ConcurrentHashMap<>();
    private static final long HISTORY_CACHE_MS = 60 * 60 * 1000;

    private static class CachedStock {
        Stock stock; long timestamp;
        CachedStock(Stock s, long t) { stock = s; timestamp = t; }
    }
    private static class CachedHistory {
        List<StockPricePoint> history; long timestamp;
        CachedHistory(List<StockPricePoint> h, long t) { history = h; timestamp = t; }
    }

    // ========== LIVE PRICE ==========
    public Stock fetchLiveStock(String symbol) {
        String upper = symbol.toUpperCase().trim();

        CachedStock c = priceCache.get(upper);
        if (c != null && (System.currentTimeMillis() - c.timestamp < PRICE_CACHE_MS)) {
            System.out.println("📦 Price cache hit: " + upper);
            return c.stock;
        }

        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + upper + "?interval=1d&range=1d";
        try {
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode meta = resp.getBody().path("chart").path("result").get(0).path("meta");

            double current = meta.path("regularMarketPrice").asDouble();
            double prev = meta.path("chartPreviousClose").asDouble();
            if (current == 0) return null;
            if (prev == 0) prev = current;

            double change = ((current - prev) / prev) * 100.0;

            Stock s = new Stock();
            s.setSymbol(upper);
            s.setName(upper);
            s.setCurrentPrice(Math.round(current * 100.0) / 100.0);
            s.setPreviousClose(Math.round(prev * 100.0) / 100.0);
            s.setChangePercent(Math.round(change * 100.0) / 100.0);

            // Simple daily signal (will be overwritten by TechnicalAnalysisService if history exists)
            if (change > 2) { s.setRecommendation("SELL"); s.setReason("Up >2% today. Possible profit-booking."); }
            else if (change < -2) { s.setRecommendation("BUY"); s.setReason("Down >2% today. Dip opportunity."); }
            else { s.setRecommendation("HOLD"); s.setReason("Sideways today. No strong momentum."); }

            priceCache.put(upper, new CachedStock(s, System.currentTimeMillis()));
            System.out.println("✅ Live price: " + upper + " = $" + current);
            return s;

        } catch (Exception e) {
            System.out.println("❌ Price fetch failed " + upper + ": " + e.getMessage());
            return null;
        }
    }

    // ========== HISTORICAL DATA ==========
    public List<StockPricePoint> fetchHistoricalData(String symbol) {
        String upper = symbol.toUpperCase().trim();

        CachedHistory c = historyCache.get(upper);
        if (c != null && (System.currentTimeMillis() - c.timestamp < HISTORY_CACHE_MS)) {
            System.out.println("📦 History cache hit: " + upper);
            return c.history;
        }

        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + upper + "?interval=1d&range=6mo";
        try {
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode result = resp.getBody().path("chart").path("result").get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);
            JsonNode closes = quote.path("close");
            JsonNode highs = quote.path("high");
            JsonNode lows = quote.path("low");
            JsonNode volumes = quote.path("volume");

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
            List<StockPricePoint> history = new ArrayList<>();

            for (int i = 0; i < timestamps.size(); i++) {
                if (closes.get(i).isNull()) continue;
                StockPricePoint p = new StockPricePoint();
                p.setDate(sdf.format(new Date(timestamps.get(i).asLong() * 1000L)));
                p.setClose(Math.round(closes.get(i).asDouble() * 100.0) / 100.0);
                p.setHigh(highs.get(i).isNull() ? p.getClose() : Math.round(highs.get(i).asDouble() * 100.0) / 100.0);
                p.setLow(lows.get(i).isNull() ? p.getClose() : Math.round(lows.get(i).asDouble() * 100.0) / 100.0);
                p.setVolume(volumes.get(i).isNull() ? 0 : volumes.get(i).asLong());
                history.add(p);
            }

            historyCache.put(upper, new CachedHistory(history, System.currentTimeMillis()));
            System.out.println("✅ History: " + upper + " (" + history.size() + " days)");
            return history;

        } catch (Exception e) {
            System.out.println("❌ History fetch failed " + upper + ": " + e.getMessage());
            return null;
        }
    }
}