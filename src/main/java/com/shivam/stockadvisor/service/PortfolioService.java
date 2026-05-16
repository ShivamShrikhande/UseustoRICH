package com.shivam.stockadvisor.service;

import com.shivam.stockadvisor.model.PortfolioItem;
import com.shivam.stockadvisor.model.Stock;
import com.shivam.stockadvisor.repository.PortfolioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StockService stockService;

    public PortfolioService(PortfolioRepository portfolioRepository, StockService stockService) {
        this.portfolioRepository = portfolioRepository;
        this.stockService = stockService;
    }

    public List<PortfolioItem> getAllItems() {
        return portfolioRepository.findAll();
    }

    public void addItem(PortfolioItem item) {
        portfolioRepository.save(item);
    }

    public void removeItem(Long id) {
        portfolioRepository.deleteById(id);
    }

    public double calculatePnL(PortfolioItem item) {
        Stock current = stockService.getStock(item.getSymbol());
        return (current.getCurrentPrice() - item.getBuyPrice()) * item.getQuantity();
    }

    public double getCurrentValue(PortfolioItem item) {
        Stock current = stockService.getStock(item.getSymbol());
        return current.getCurrentPrice() * item.getQuantity();
    }

    public double getInvestedValue(PortfolioItem item) {
        return item.getBuyPrice() * item.getQuantity();
    }
}