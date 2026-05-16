package com.shivam.stockadvisor.service;

import com.shivam.stockadvisor.model.Stock;
import com.shivam.stockadvisor.model.StockPricePoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure math engine. Calculates RSI, SMA, MACD from historical prices.
 * No API calls here — just calculations.
 */
@Service
public class TechnicalAnalysisService {

    public void calculateIndicators(Stock stock, List<StockPricePoint> history) {
        if (history == null || history.size() < 50) return;

        // Extract closing prices
        List<Double> closes = new ArrayList<>();
        for (StockPricePoint p : history) {
            closes.add(p.getClose());
        }

        // Calculate indicators
        stock.setSma20(calculateSMA(closes, 20));
        stock.setSma50(calculateSMA(closes, 50));
        stock.setRsi14(calculateRSI(closes, 14));

        double[] macd = calculateMACD(closes);
        stock.setMacdLine(macd[0]);
        stock.setMacdSignal(macd[1]);
        stock.setMacdHistogram(macd[2]);

        // Generate smart signal based on ALL indicators combined
        generateSmartSignal(stock);
    }

    // Simple Moving Average
    private double calculateSMA(List<Double> closes, int period) {
        if (closes.size() < period) return 0;
        double sum = 0;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            sum += closes.get(i);
        }
        return Math.round((sum / period) * 100.0) / 100.0;
    }

    // Relative Strength Index (Wilder's method)
    private double calculateRSI(List<Double> closes, int period) {
        if (closes.size() < period + 1) return 50.0;

        double avgGain = 0, avgLoss = 0;

        // Initial averages
        for (int i = 1; i <= period; i++) {
            double change = closes.get(i) - closes.get(i - 1);
            if (change > 0) avgGain += change;
            else avgLoss += Math.abs(change);
        }
        avgGain /= period;
        avgLoss /= period;

        // Smoothing
        for (int i = period + 1; i < closes.size(); i++) {
            double change = closes.get(i) - closes.get(i - 1);
            double gain = change > 0 ? change : 0;
            double loss = change < 0 ? Math.abs(change) : 0;
            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;
        }

        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        return Math.round(rsi * 100.0) / 100.0;
    }

    // MACD = EMA12 - EMA26, Signal = EMA9 of MACD, Histogram = MACD - Signal
    private double[] calculateMACD(List<Double> closes) {
        if (closes.size() < 26) return new double[]{0, 0, 0};

        double ema12 = calculateEMA(closes, 12);
        double ema26 = calculateEMA(closes, 26);
        double macdLine = ema12 - ema26;

        // Build MACD history for signal line
        List<Double> macdHistory = new ArrayList<>();
        for (int i = 26; i < closes.size(); i++) {
            List<Double> sub = closes.subList(0, i + 1);
            double e12 = calculateEMA(sub, 12);
            double e26 = calculateEMA(sub, 26);
            macdHistory.add(e12 - e26);
        }

        double signalLine = 0;
        if (macdHistory.size() >= 9) {
            signalLine = calculateEMA(macdHistory, 9);
        } else if (!macdHistory.isEmpty()) {
            double sum = 0;
            for (double v : macdHistory) sum += v;
            signalLine = sum / macdHistory.size();
        }

        double histogram = macdLine - signalLine;
        return new double[]{
                Math.round(macdLine * 100.0) / 100.0,
                Math.round(signalLine * 100.0) / 100.0,
                Math.round(histogram * 100.0) / 100.0
        };
    }

    // Exponential Moving Average
    private double calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) return prices.get(prices.size() - 1);
        double k = 2.0 / (period + 1.0);
        double ema = prices.get(0);
        for (int i = 1; i < prices.size(); i++) {
            ema = (prices.get(i) * k) + (ema * (1 - k));
        }
        return ema;
    }

    // Multi-factor scoring system
    private void generateSmartSignal(Stock stock) {
        double price = stock.getCurrentPrice();
        double sma20 = stock.getSma20();
        double sma50 = stock.getSma50();
        double rsi = stock.getRsi14();
        double macdHist = stock.getMacdHistogram();

        int buyScore = 0;
        int sellScore = 0;
        StringBuilder reason = new StringBuilder();

        // 1. Trend (SMA Crossover)
        if (sma20 > sma50) {
            buyScore += 2;
            reason.append("Golden Cross (SMA20>SMA50). ");
        } else {
            sellScore += 2;
            reason.append("Death Cross (SMA20<SMA50). ");
        }

        // 2. Price vs short-term average
        if (price > sma20) {
            buyScore += 1;
            reason.append("Price above SMA20. ");
        } else {
            sellScore += 1;
            reason.append("Price below SMA20. ");
        }

        // 3. RSI Momentum
        if (rsi < 30) {
            buyScore += 2;
            reason.append("RSI oversold(").append(rsi).append("). ");
        } else if (rsi > 70) {
            sellScore += 2;
            reason.append("RSI overbought(").append(rsi).append("). ");
        } else {
            reason.append("RSI neutral(").append(rsi).append("). ");
        }

        // 4. MACD Histogram
        if (macdHist > 0) {
            buyScore += 1;
            reason.append("MACD bullish.");
        } else {
            sellScore += 1;
            reason.append("MACD bearish.");
        }

        // Final verdict
        if (buyScore >= sellScore + 2) {
            stock.setRecommendation("BUY");
        } else if (sellScore >= buyScore + 2) {
            stock.setRecommendation("SELL");
        } else {
            stock.setRecommendation("HOLD");
        }
        stock.setReason(reason.toString());
    }
}