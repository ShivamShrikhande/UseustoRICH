package com.shivam.stockadvisor.model;

/**
 * One day of stock data (for chart + indicator calculation).
 */
public class StockPricePoint {
    private String date;    // e.g., "Jan 15"
    private double close;   // Closing price
    private double high;
    private double low;
    private long volume;

    public StockPricePoint() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }
}