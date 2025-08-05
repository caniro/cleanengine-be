package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.Order;
import com.cleanengine.coin.order.domain.OrderType;
import com.cleanengine.coin.order.domain.SellOrder;
import com.cleanengine.coin.order.domain.spi.WaitingOrders;
import com.cleanengine.coin.trade.domain.model.TradePair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class TradeMatcher {

    // 1초마다 로깅
    private long lastLogTime = 0;

    private static final long LOG_INTERVAL = 1000;

    public Optional<TradePair<Order, Order>> matchOrders(WaitingOrders waitingOrders) {
        this.writeQueueLog(waitingOrders);

        TradePair<Order, Order> targetTradePair;

        // 시장가 주문 우선처리
        SellOrder marketSellOrder = waitingOrders.getSellOrderPriorityQueueStore(OrderType.MARKET).peek();
        SellOrder limitSellOrder = waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).peek();
        BuyOrder marketBuyOrder = waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).peek();
        BuyOrder limitBuyOrder = waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).peek();

        if (marketSellOrder != null && limitBuyOrder != null) {
            // 1. 시장가 매도 주문, 지정가 매수 주문
            targetTradePair = new TradePair<>(marketSellOrder, limitBuyOrder);
        } else if (marketBuyOrder != null && limitSellOrder != null) {
            // 2. 시장가 매수 주문, 지정가 매도 주문
            targetTradePair = new TradePair<>(marketBuyOrder, limitSellOrder);
        } else {
            // 3. 지정가 주문
            targetTradePair = this.matchBetweenLimitOrders(limitBuyOrder, limitSellOrder);
        }

        return Optional.ofNullable(targetTradePair);
    }

    private TradePair<Order, Order> matchBetweenLimitOrders(BuyOrder limitBuyOrder, SellOrder limitSellOrder) {
        if (limitSellOrder == null || limitBuyOrder == null)
            return null;

        if (this.canMatch(limitBuyOrder, limitSellOrder))
            return new TradePair<>(limitBuyOrder, limitSellOrder);
        else
            return null;
    }

    private boolean canMatch(BuyOrder buyOrder, SellOrder sellOrder) {
        return buyOrder.getPrice() >= sellOrder.getPrice();
    }

    private void writeQueueLog(WaitingOrders waitingOrders) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > LOG_INTERVAL && log.isDebugEnabled()) {
            log.debug("주문 큐 - 시장가매도[{}], 지정가매도[{}], 시장가매수[{}], 지정가매수[{}]",
                    waitingOrders.getSellOrderPriorityQueueStore(OrderType.MARKET).size(),
                    waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).size(),
                    waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).size(),
                    waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).size());
            lastLogTime = currentTime;
        }
    }

}
