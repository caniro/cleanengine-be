package com.cleanengine.coin.mypage.infra;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CurrentPriceCache {
    private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();

    public void update(String ticker, double price){
        currentPrices.put(ticker, price);
    }
    public double getCurrentPrice(String ticker){
        return currentPrices.getOrDefault(ticker,0.0);
    }
    public Map<String, Double> getCurrentPrices(){
        return currentPrices;
    }
}
