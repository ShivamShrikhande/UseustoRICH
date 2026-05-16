package com.shivam.stockadvisor.service;

import com.shivam.stockadvisor.model.Stock;
import com.shivam.stockadvisor.model.StockPricePoint;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StockService {

    private final Map<String, Stock> stockDatabase = new HashMap<>();
    private final YahooFinanceService yahooFinanceService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final AiAnalysisService aiAnalysisService;   // ← NEW

    public StockService(YahooFinanceService yahooFinanceService,
                        TechnicalAnalysisService technicalAnalysisService,
                        AiAnalysisService aiAnalysisService) {   // ← NEW param
        this.yahooFinanceService = yahooFinanceService;
        this.technicalAnalysisService = technicalAnalysisService;
        this.aiAnalysisService = aiAnalysisService;   // ← NEW

        stockDatabase.put("AAPL", new Stock("AAPL", "Apple Inc.", 189.52, 187.43, 1.11, "HOLD", "Tech giant."));
        stockDatabase.put("GOOGL", new Stock("GOOGL", "Alphabet Inc.", 175.23, 172.80, 1.41, "BUY", "AI growth."));
        stockDatabase.put("TSLA", new Stock("TSLA", "Tesla Inc.", 178.45, 185.20, -3.65, "SELL", "Margin pressure."));
        stockDatabase.put("MSFT", new Stock("MSFT", "Microsoft Corp.", 420.15, 415.30, 1.17, "BUY", "Azure dominance."));
        stockDatabase.put("AMZN", new Stock("AMZN", "Amazon.com Inc.", 185.90, 182.45, 1.89, "BUY", "AWS + E-commerce."));
        stockDatabase.put("NVDA", new Stock("NVDA", "NVIDIA Corp.", 950.20, 940.50, 1.03, "HOLD", "AI chip leader."));
        stockDatabase.put("INFY", new Stock("INFY", "Infosys Ltd", 1520.50, 1505.30, 1.01, "HOLD", "Indian IT steady."));
        stockDatabase.put("RELIANCE.NS", new Stock("RELIANCE.NS", "Reliance Industries", 2850.75, 2830.20, 0.73, "BUY", "Energy-to-tech."));
    }

    public List<Stock> getAllStocks() {
        List<Stock> result = new ArrayList<>();
        for (String symbol : stockDatabase.keySet()) {
            Stock live = yahooFinanceService.fetchLiveStock(symbol);
            if (live != null) {
                live.setName(stockDatabase.get(symbol).getName());
                result.add(live);
            } else {
                Stock fallback = stockDatabase.get(symbol);
                enrichWithSyntheticAnalysis(fallback, symbol);
                result.add(fallback);
            }
        }
        return result;
    }

    public Stock getStock(String symbol) {
        String upper = symbol.toUpperCase().trim();

        Stock live = yahooFinanceService.fetchLiveStock(upper);
        List<StockPricePoint> history = yahooFinanceService.fetchHistoricalData(upper);

        if (live != null && history != null && history.size() >= 50) {
            if (stockDatabase.containsKey(upper)) {
                live.setName(stockDatabase.get(upper).getName());
            }
            technicalAnalysisService.calculateIndicators(live, history);
            aiAnalysisService.generateExplanation(live);   // ← NEW: Generate AI text
            live.setPriceHistory(history.subList(Math.max(0, history.size() - 30), history.size()));
            return live;
        }

        if (stockDatabase.containsKey(upper)) {
            Stock fallback = stockDatabase.get(upper);
            enrichWithSyntheticAnalysis(fallback, upper);
            aiAnalysisService.generateExplanation(fallback);   // ← NEW: Generate AI text
            return fallback;
        }

        Stock s = new Stock();
        s.setSymbol(upper);
        s.setName(upper + " (Simulated)");
        double rp = 100 + Math.random() * 900;
        s.setCurrentPrice(rp);
        s.setPreviousClose(rp * 0.98);
        s.setChangePercent((Math.random() * 6) - 3);
        s.setRecommendation("HOLD");
        s.setReason("No data for " + upper + ". Check symbol spelling.");
        return s;
    }

    private void enrichWithSyntheticAnalysis(Stock stock, String symbol) {
        List<StockPricePoint> history = generateSyntheticHistory(stock.getCurrentPrice(), symbol, 60);
        technicalAnalysisService.calculateIndicators(stock, history);
        stock.setPriceHistory(history.subList(history.size() - 30, history.size()));
    }

    private List<StockPricePoint> generateSyntheticHistory(double basePrice, String symbol, int days) {
        List<StockPricePoint> history = new ArrayList<>();
        Random random = new Random(symbol.hashCode());
        double price = basePrice * (0.85 + random.nextDouble() * 0.10);
        double step = (basePrice - price) / days;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");

        for (int i = 0; i < days; i++) {
            double noise = (random.nextDouble() - 0.5) * 0.03;
            price = price * (1 + noise) + step;
            price = Math.max(basePrice * 0.75, Math.min(basePrice * 1.25, price));

            StockPricePoint p = new StockPricePoint();
            p.setDate(sdf.format(cal.getTime()));
            p.setClose(Math.round(price * 100.0) / 100.0);
            p.setHigh(Math.round(price * (1.01 + random.nextDouble() * 0.02) * 100.0) / 100.0);
            p.setLow(Math.round(price * (0.97 + random.nextDouble() * 0.02) * 100.0) / 100.0);
            p.setVolume(1000000 + random.nextInt(9000000));
            history.add(p);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return history;
    }
}