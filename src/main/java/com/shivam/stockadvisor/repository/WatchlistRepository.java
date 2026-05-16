package com.shivam.stockadvisor.repository;

import com.shivam.stockadvisor.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
boolean existsBySymbol(String symbol);
}