package com.shivam.stockadvisor.repository;

import com.shivam.stockadvisor.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioItem, Long> {
        }