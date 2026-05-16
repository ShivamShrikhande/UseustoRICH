package com.shivam.stockadvisor.model;

import java.util.List;

public class Stock {

    private String symbol;
    private String name;
    private double currentPrice;
    private double previousClose;
    private double changePercent;
    private String recommendation;
    private String reason;
    private String aiExplanation;        // ← NEW

    private double sma20;
    private double sma50;
    private double rsi14;
    private double macdLine;
    private double macdSignal;
    private double macdHistogram;
    private List<StockPricePoint> priceHistory;

    public Stock() {}

    public Stock(String symbol, String name, double currentPrice, double previousClose,
                 double changePercent, String recommendation, String reason) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.previousClose = previousClose;
        this.changePercent = changePercent;
        this.recommendation = recommendation;
        this.reason = reason;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getPreviousClose() { return previousClose; }
    public void setPreviousClose(double previousClose) { this.previousClose = previousClose; }

    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getAiExplanation() { return aiExplanation; }          // ← NEW
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }  // ← NEW

    public double getSma20() { return sma20; }
    public void setSma20(double sma20) { this.sma20 = sma20; }

    public double getSma50() { return sma50; }
    public void setSma50(double sma50) { this.sma50 = sma50; }

    public double getRsi14() { return rsi14; }
    public void setRsi14(double rsi14) { this.rsi14 = rsi14; }

    public double getMacdLine() { return macdLine; }
    public void setMacdLine(double macdLine) { this.macdLine = macdLine; }

    public double getMacdSignal() { return macdSignal; }
    public void setMacdSignal(double macdSignal) { this.macdSignal = macdSignal; }

    public double getMacdHistogram() { return macdHistogram; }
    public void setMacdHistogram(double macdHistogram) { this.macdHistogram = macdHistogram; }

    public List<StockPricePoint> getPriceHistory() { return priceHistory; }
    public void setPriceHistory(List<StockPricePoint> priceHistory) { this.priceHistory = priceHistory; }
}