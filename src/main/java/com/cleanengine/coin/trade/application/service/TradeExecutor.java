package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.Order;
import com.cleanengine.coin.order.domain.OrderStatus;
import com.cleanengine.coin.order.domain.SellOrder;
import com.cleanengine.coin.order.domain.spi.WaitingOrders;
import com.cleanengine.coin.trade.application.port.out.TradeExecutedEventPublisherPort;
import com.cleanengine.coin.trade.application.port.out.TradeOrderCompletedEventPublisherPort;
import com.cleanengine.coin.trade.domain.event.TradeExecutedEvent;
import com.cleanengine.coin.trade.domain.event.TradeOrderCompletedEvent;
import com.cleanengine.coin.trade.domain.exception.TradeZeroOrderException;
import com.cleanengine.coin.trade.domain.model.Trade;
import com.cleanengine.coin.trade.domain.model.TradePair;
import com.cleanengine.coin.user.info.application.AccountService;
import com.cleanengine.coin.user.info.application.WalletService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.cleanengine.coin.common.CommonValues.approxEquals;

@Slf4j
@RequiredArgsConstructor
@Component
public class TradeExecutor {

    private final WalletService walletService;

    private final AccountService accountService;

    @Getter
    private final TradeExecutedEventPublisherPort tradeExecutedEventPublisher;

    private final TradeOrderCompletedEventPublisherPort tradeOrderCompletedEventPublisher;

    private final TradeService tradeService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Trade executeTrade(WaitingOrders waitingOrders, TradePair<Order, Order> tradePair, String ticker) {
        BuyOrder buyOrder = tradePair.getBuyOrder();
        SellOrder sellOrder = tradePair.getSellOrder();
        log.trace("{} - 체결 시작: 매수[{} {}원 {}개] / 매도[{} {}원 {}개]", ticker, buyOrder.getId(), buyOrder.getPrice(), buyOrder.getRemainingSize(),
                sellOrder.getId(), sellOrder.getPrice(), sellOrder.getRemainingSize());

        double tradedPrice;
        double tradedSize;
        double totalTradedPrice;

        // 체결 단가, 수량 확정
        TradeUnitPriceAndSize tradeUnitPriceAndSize = getTradeUnitPriceAndSize(buyOrder, sellOrder);
        tradedSize = tradeUnitPriceAndSize.tradedSize();
        tradedPrice = tradeUnitPriceAndSize.tradedPrice();
        if (approxEquals(tradedSize, 0.0)) {
            checkZeroOrderAndThrowException(buyOrder, sellOrder);
        }
        writeTradingLog(buyOrder, sellOrder);

        totalTradedPrice = tradedPrice * tradedSize;
        // 주문 잔여수량, 잔여금액 감소
        if (isMarketOrder(buyOrder))
            buyOrder.decreaseRemainingDeposit(totalTradedPrice);
        else
            buyOrder.decreaseRemainingSize(tradedSize);
        sellOrder.decreaseRemainingSize(tradedSize);

        // 주문 완전체결 처리(잔여금액 or 잔여수량이 0)
        removeCompletedOrders(waitingOrders, buyOrder, sellOrder);

        tradeService.updateOrder(buyOrder);
        tradeService.updateOrder(sellOrder);

        // 체결내역 저장
        Trade trade = Trade.of(ticker, LocalDateTime.now(), buyOrder.getUserId(), sellOrder.getUserId(), tradedPrice, tradedSize);

        TradeExecutedEvent tradeExecutedEvent = TradeExecutedEvent.of(trade, buyOrder.getId(), sellOrder.getId());
        tradeExecutedEventPublisher.publish(tradeExecutedEvent);

        if (buyOrder.getIsBot() == false || sellOrder.getIsBot() == false) {
            // 지갑 누적계산
            walletService.updateWalletAfterTrade(buyOrder.getUserId(), ticker, tradedPrice, tradedSize);

            // 예수금 처리
            //   - 매도 예수금 처리
            this.increaseAccountCash(sellOrder, totalTradedPrice);

            //   - 매수 잔여금액 반환
            if (!isMarketOrder(buyOrder) && buyOrder.getPrice() > tradedPrice) {
                double totalRefundAmount = (buyOrder.getPrice() - tradedPrice) * tradedSize;
                this.increaseAccountCash(buyOrder, totalRefundAmount);
                log.debug("[{}] 유저 {} 의 차액 반환 : {}원 (매도 호가보다 높은 가격에 매수를 시도)", Thread.currentThread().threadId(), buyOrder.getUserId(), totalRefundAmount);
            }
        }

        return trade;
    }

    private static void checkZeroOrderAndThrowException(BuyOrder buyOrder, SellOrder sellOrder) {
        Order zeroOrder = null;
        if (approxEquals(buyOrder.getRemainingDeposit(), 0.0))
            zeroOrder = buyOrder;
        else if (approxEquals(sellOrder.getRemainingSize(), 0.0))
            zeroOrder = sellOrder;
        if (zeroOrder == null)
            throw new RuntimeException("수량이 0인 주문이 없는데도 체결 수량이 0인 현상 발생");
        throw new TradeZeroOrderException(String.format("체결 중단: 체결 수량이 0! 매수단가 : %s, 매도단가 : %s",
                buyOrder.getPrice(), sellOrder.getPrice()), zeroOrder);
    }

    private void increaseAccountCash(Order order, Double amount) {
        int updatedRows = accountService.increaseAccountCash(order.getUserId(), amount);

        if (updatedRows == 0) {
            throw new RuntimeException("account updatedRows == 0");
        }
    }

    private static TradeUnitPriceAndSize getTradeUnitPriceAndSize(BuyOrder buyOrder, SellOrder sellOrder) {
        double tradedPrice;
        double tradedSize;
        if (isMarketOrder(buyOrder)) { // 시장가매수-지정가매도
            tradedPrice = sellOrder.getPrice();
            if (buyOrder.getRemainingDeposit() >= tradedPrice * sellOrder.getRemainingSize()) { // 매수 잔여예수금이 매도 잔여량보다 크거나 같은 경우 (매수 부분체결 or 완전체결, 매도 완전체결)
                tradedSize = sellOrder.getRemainingSize();
            } else {
                tradedSize = buyOrder.getRemainingDeposit() / tradedPrice;
            }
        } else if (isMarketOrder(sellOrder)) { // 시장가매도-지정가매수
            tradedPrice = buyOrder.getPrice();
            tradedSize = Math.min(sellOrder.getRemainingSize(), buyOrder.getRemainingSize());
        } else { // 지정가매수-지정가매도
            tradedPrice = getTradedUnitPrice(buyOrder, sellOrder);
            tradedSize = Math.min(buyOrder.getRemainingSize(), sellOrder.getRemainingSize());
        }
        return new TradeUnitPriceAndSize(tradedSize, tradedPrice);
    }

    private record TradeUnitPriceAndSize(double tradedSize, double tradedPrice) {

    }

    private static double getTradedUnitPrice(BuyOrder buyOrder, SellOrder sellOrder) {
        // 주문 시간을 비교하여 먼저 들어온 주문의 가격으로 거래
        if (buyOrder.getCreatedAt().isBefore(sellOrder.getCreatedAt())) {
            return buyOrder.getPrice();
        } else {
            return sellOrder.getPrice();
        }
    }

    private static void writeTradingLog(BuyOrder buyOrder, SellOrder sellOrder) {
        log.debug("[{}] 체결 확정!  종목: {}, ({}: {}가 {}로 {}만큼 매수주문), ({}: {}가 {}로 {}만큼 매도주문)",
                Thread.currentThread().threadId(),
                buyOrder.getTicker(),
                buyOrder.getId(),
                buyOrder.getUserId(),
                isMarketOrder(buyOrder) ? "시장가" : "지정가(" + buyOrder.getPrice() + "원)",
                buyOrder.getRemainingSize() == null ? buyOrder.getRemainingDeposit() : buyOrder.getRemainingSize(),
                sellOrder.getId(),
                sellOrder.getUserId(),
                isMarketOrder(sellOrder) ? "시장가" : "지정가(" + sellOrder.getPrice() + "원)",
                sellOrder.getRemainingSize());
    }

    private void removeCompletedOrders(WaitingOrders waitingOrders, BuyOrder buyOrder, SellOrder sellOrder) {
        removeCompletedOrder(waitingOrders, buyOrder);
        removeCompletedOrder(waitingOrders, sellOrder);
    }

    private void removeCompletedOrder(WaitingOrders waitingOrders, Order order) {
        boolean isOrderCompleted = false;

        if (order instanceof BuyOrder buyOrder) {
            isOrderCompleted = (isMarketOrder(buyOrder) && approxEquals(buyOrder.getRemainingDeposit(), 0.0)) ||
                    (isLimitOrder(buyOrder) && approxEquals(buyOrder.getRemainingSize(), 0.0));
        } else if (order instanceof SellOrder sellOrder) {
            isOrderCompleted = approxEquals(sellOrder.getRemainingSize(), 0.0);
        }

        if (isOrderCompleted) {
            waitingOrders.removeOrder(order);
            updateCompletedOrderStatus(order);
            publishOrderCompletionEvent(order);
        }
    }

    private void publishOrderCompletionEvent(Order order) {
        tradeOrderCompletedEventPublisher.publish(TradeOrderCompletedEvent.of(order));
    }

    private static void updateCompletedOrderStatus(Order order) {
        order.setState(OrderStatus.DONE);
    }

    private static boolean isMarketOrder(Order order) {
        return order.getIsMarketOrder();
    }

    private static boolean isLimitOrder(Order order) {
        return !order.getIsMarketOrder();
    }

}
