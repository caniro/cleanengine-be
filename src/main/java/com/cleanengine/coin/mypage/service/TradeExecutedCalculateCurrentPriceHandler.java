package com.cleanengine.coin.mypage.service;

import com.cleanengine.coin.mypage.infra.CurrentPriceCache;
import com.cleanengine.coin.trade.domain.event.TradeExecutedEvent;
import com.cleanengine.coin.trade.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeExecutedCalculateCurrentPriceHandler {

    private final CurrentPriceCache currentPriceCache;

    @EventListener
    public void onTradeExecuted(TradeExecutedEvent event) {
        Trade trade = event.getTrade();
        currentPriceCache.update(trade.getTicker(), trade.getPrice());
    }

}
