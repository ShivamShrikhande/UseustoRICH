package com.shivam.stockadvisor.controller;

import com.shivam.stockadvisor.model.PortfolioItem;
import com.shivam.stockadvisor.model.Stock;
import com.shivam.stockadvisor.model.StockPricePoint;
import com.shivam.stockadvisor.model.WatchlistItem;
import com.shivam.stockadvisor.repository.PortfolioRepository;
import com.shivam.stockadvisor.repository.WatchlistRepository;
import com.shivam.stockadvisor.service.PortfolioService;
import com.shivam.stockadvisor.service.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StockController {

    private final StockService stockService;
    private final WatchlistRepository watchlistRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioService portfolioService;

    public StockController(StockService stockService,
                           WatchlistRepository watchlistRepository,
                           PortfolioRepository portfolioRepository,
                           PortfolioService portfolioService) {
        this.stockService = stockService;
        this.watchlistRepository = watchlistRepository;
        this.portfolioRepository = portfolioRepository;
        this.portfolioService = portfolioService;
    }

    // ========== HOME ==========
    @GetMapping("/")
    public String home(Model model) {
        List<Stock> stocks = stockService.getAllStocks();
        model.addAttribute("stocks", stocks);
        model.addAttribute("searchSymbol", "");
        return "index";
    }

    @PostMapping("/search")
    public String search(@RequestParam String symbol, Model model) {
        Stock stock = stockService.getStock(symbol);
        List<Stock> stocks = stockService.getAllStocks();

        model.addAttribute("stocks", stocks);
        model.addAttribute("searchedStock", stock);
        model.addAttribute("searchSymbol", symbol);

        if (stock.getPriceHistory() != null && !stock.getPriceHistory().isEmpty()) {
            List<String> dates = new ArrayList<>();
            List<Double> prices = new ArrayList<>();
            for (StockPricePoint p : stock.getPriceHistory()) {
                dates.add(p.getDate());
                prices.add(p.getClose());
            }
            model.addAttribute("chartDates", dates);
            model.addAttribute("chartPrices", prices);
        }

        return "index";
    }

    // ========== WATCHLIST ==========
    @GetMapping("/watchlist")
    public String watchlist(Model model) {
        List<WatchlistItem> items = watchlistRepository.findAll();
        List<Map<String, Object>> enriched = new ArrayList<>();

        for (WatchlistItem item : items) {
            Stock stock = stockService.getStock(item.getSymbol());
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("symbol", item.getSymbol());
            map.put("name", item.getName());
            map.put("stock", stock);
            enriched.add(map);
        }

        model.addAttribute("watchlist", enriched);
        model.addAttribute("popularStocks", stockService.getAllStocks());
        return "watchlist";
    }

    @PostMapping("/watchlist/add")
    public String addToWatchlist(@RequestParam String symbol, @RequestParam String name) {
        String upper = symbol.toUpperCase().trim();
        if (!watchlistRepository.existsBySymbol(upper)) {
            WatchlistItem item = new WatchlistItem();
            item.setSymbol(upper);
            item.setName(name);
            watchlistRepository.save(item);
        }
        return "redirect:/watchlist";
    }

    @PostMapping("/watchlist/remove/{id}")
    public String removeFromWatchlist(@PathVariable Long id) {
        watchlistRepository.deleteById(id);
        return "redirect:/watchlist";
    }

    // ========== PORTFOLIO ==========
    @GetMapping("/portfolio")
    public String portfolio(Model model) {
        List<PortfolioItem> items = portfolioService.getAllItems();
        List<Map<String, Object>> enriched = new ArrayList<>();

        double totalInvested = 0;
        double totalCurrent = 0;

        for (PortfolioItem item : items) {
            Stock stock = stockService.getStock(item.getSymbol());
            double invested = portfolioService.getInvestedValue(item);
            double current = portfolioService.getCurrentValue(item);
            double pnl = portfolioService.calculatePnL(item);

            totalInvested += invested;
            totalCurrent += current;

            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("symbol", item.getSymbol());
            map.put("name", item.getName());
            map.put("buyPrice", item.getBuyPrice());
            map.put("quantity", item.getQuantity());
            map.put("buyDate", item.getBuyDate());
            map.put("currentPrice", stock.getCurrentPrice());
            map.put("invested", invested);
            map.put("currentValue", current);
            map.put("pnl", pnl);
            map.put("pnlPercent", invested > 0 ? (pnl / invested) * 100 : 0);
            map.put("recommendation", stock.getRecommendation());
            enriched.add(map);
        }

        model.addAttribute("portfolio", enriched);
        model.addAttribute("totalInvested", totalInvested);
        model.addAttribute("totalCurrent", totalCurrent);
        model.addAttribute("totalPnl", totalCurrent - totalInvested);
        model.addAttribute("popularStocks", stockService.getAllStocks());
        return "portfolio";
    }

    @PostMapping("/portfolio/add")
    public String addToPortfolio(@RequestParam String symbol,
                                 @RequestParam String name,
                                 @RequestParam double buyPrice,
                                 @RequestParam int quantity,
                                 @RequestParam String buyDate) {
        PortfolioItem item = new PortfolioItem();
        item.setSymbol(symbol.toUpperCase().trim());
        item.setName(name);
        item.setBuyPrice(buyPrice);
        item.setQuantity(quantity);
        item.setBuyDate(LocalDate.parse(buyDate));
        portfolioRepository.save(item);
        return "redirect:/portfolio";
    }

    @PostMapping("/portfolio/remove/{id}")
    public String removeFromPortfolio(@PathVariable Long id) {
        portfolioRepository.deleteById(id);
        return "redirect:/portfolio";
    }
}