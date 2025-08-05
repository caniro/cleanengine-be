package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.order.adapter.out.persistentce.order.command.BuyOrderRepository;
import com.cleanengine.coin.order.adapter.out.persistentce.order.command.SellOrderRepository;
import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.OrderType;
import com.cleanengine.coin.order.domain.SellOrder;
import com.cleanengine.coin.order.domain.spi.WaitingOrders;
import com.cleanengine.coin.order.domain.spi.WaitingOrdersManager;
import com.cleanengine.coin.trade.adapter.out.persistence.JpaTradeCommandRepository;
import com.cleanengine.coin.trade.application.port.in.TradeQueryUseCase;
import com.cleanengine.coin.trade.domain.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"dev", "it", "h2-mem"})
@SpringBootTest
@DisplayName("체결처리 h2 통합테스트")
@Disabled
class TradeFlowServiceIntegrationTest {

    private static final double MINIMUM_ORDER_SIZE = 0.00000001;

    @Autowired
    BuyOrderRepository buyOrderRepository;

    @Autowired
    SellOrderRepository sellOrderRepository;

    @Autowired
    TradeQueryUseCase tradeQueryUseCase;

    @Autowired
    JpaTradeCommandRepository jpaTradeCommandRepository;

    @Autowired
    private WaitingOrdersManager waitingOrdersManager;

    private final String ticker = "BTC";

    private final WaitingOrders waitingOrders = waitingOrdersManager.getWaitingOrders(ticker);

    @BeforeEach
    void setUp() {
        WaitingOrders waitingOrders = waitingOrdersManager.getWaitingOrders(ticker);
        waitingOrders.clearAllQueues();
        jpaTradeCommandRepository.deleteAll();
        buyOrderRepository.deleteAll();
        sellOrderRepository.deleteAll();
    }

    // TODO : 모든 케이스에서 각 객체의 값까지 정합성이 맞는지 테스트 필요

    @DisplayName("지정가매수-지정가매도 완전체결")
    @Test
    void testLimitToLimitCompleteTrade() {
        double orderSize = 10.0;
        double price = 130_000_000.0;
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, ticker, 1, orderSize, price, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, ticker, 2, orderSize, price, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> tradeOfBuy = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> tradeOfSell = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(tradeOfBuy, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, tradeOfBuy.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(tradeOfSell, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, tradeOfSell.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(
                tradeOfBuy.getFirst().getId(),
                tradeOfSell.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertTrue(tradeOfBuy.getFirst().getSize() - orderSize < MINIMUM_ORDER_SIZE, "체결수량과 주문수량은 같아야 합니다.");
        assertTrue(tradeOfBuy.getFirst().getPrice() - price < MINIMUM_ORDER_SIZE, "체결단가와 주문단가는 같아야 합니다.");

        assertTrue(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매수 주문이 없어야 합니다.");
        assertTrue(waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매도 주문이 없어야 합니다.");

        assertTrue(buyOrder.getRemainingSize() < MINIMUM_ORDER_SIZE, "매수주문의 잔여수량은 없어야 합니다.");
        assertTrue(sellOrder.getRemainingSize() < MINIMUM_ORDER_SIZE, "매도주문의 잔여수량은 없어야 합니다.");
    }

    @DisplayName("지정가매수-지정가매도 매도부분체결")
    @Test
    void testLimitToLimitPartialTrade1() {
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, ticker, 1, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, ticker, 2, 10.0, 130_000_000.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertTrue(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매수 주문이 없어야 합니다.");
        assertEquals(1, waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).size(), "지정가 매도 주문이 1개 남아있어야 합니다.");
    }

    @DisplayName("지정가매수-지정가매도 매수부분체결")
    @Test
    void testLimitToLimitPartialTrade2() {
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, ticker, 1, 10.0, 130_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, ticker, 2, 5.0, 130_000_000.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertEquals(1, waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).size(), "지정가 매수 주문이 1개 남아있어야 합니다.");
        assertTrue(waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매도 주문이 없어야 합니다.");
    }

    @DisplayName("시장가매수-지정가매도 완전체결")
    @Test
    void testMarketToLimitCompleteTrade1() {
        BuyOrder buyOrder = BuyOrder.createMarketBuyOrder(1L, ticker, 1, 1_300_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, ticker, 2, 10.0, 130_000_000.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertTrue(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).isEmpty(), "남은 시장가 매수 주문이 없어야 합니다.");
        assertTrue(waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매도 주문이 없어야 합니다.");
    }

    @DisplayName("지정가매수-시장가매도 완전체결")
    @Test
    void testMarketToLimitCompleteTrade2() {
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, ticker, 1, 10.0, 130_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createMarketSellOrder(1L, ticker, 2, 10.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertTrue(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매수 주문이 없어야 합니다.");
        assertTrue(waitingOrders.getSellOrderPriorityQueueStore(OrderType.MARKET).isEmpty(), "남은 시장가 매도 주문이 없어야 합니다.");
    }

    @DisplayName("시장가매수-지정가매도 매도부분체결")
    @Test
    void testMarketToLimitPartialTrade1() {
        BuyOrder buyOrder = BuyOrder.createMarketBuyOrder(1L, ticker, 1, 130_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, ticker, 2, 10.0, 130_000_000.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertTrue(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).isEmpty(), "남은 시장가 매수 주문이 없어야 합니다.");
        assertEquals(1, waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).size(), "지정가 매도 주문이 1개 남아있어야 합니다.");
    }

    @DisplayName("시장가매수-지정가매도 매수부분체결")
    @Test
    void testMarketToLimitPartialTrade2() {
        BuyOrder buyOrder = BuyOrder.createMarketBuyOrder(1L, ticker, 1, 1_300_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, ticker, 2, 1.0, 130_000_000.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertEquals(1, waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).size(), "시장가 매수 주문이 1개 남아있어야 합니다.");
        assertTrue(waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "남은 지정가 매도 주문이 없어야 합니다.");

        assert waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).peek() != null;
        assertEquals(1_300_000_000.0 - 130_000_000.0,
                waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET).peek().getRemainingDeposit(),
                "잔여 예수금이 맞지 않습니다.");
    }

    @DisplayName("지정가매수-시장가매도 매수부분체결")
    @Test
    void testMarketToLimitPartialTrade3() {
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, ticker, 1, 10.0, 130_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createMarketSellOrder(1L, ticker, 2, 1.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertEquals(1, waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).size(), "지정가 매수 주문이 1개 남아있어야 합니다.");
        assertTrue(waitingOrders.getSellOrderPriorityQueueStore(OrderType.MARKET).isEmpty(), "남은 시장가 매도 주문이 없어야 합니다.");
    }

    @DisplayName("지정가매수-시장가매도 매도부분체결")
    @Test
    void testMarketToLimitPartialTrade4() {
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, ticker, 1, 1.0, 130_000_000.0, LocalDateTime.now(), false);
        SellOrder sellOrder = SellOrder.createMarketSellOrder(1L, ticker, 2, 10.0, LocalDateTime.now(), false);

        buyOrderRepository.save(buyOrder);
        sellOrderRepository.save(sellOrder);

        waitingOrders.addOrder(buyOrder);
        waitingOrders.addOrder(sellOrder);

        // 체결이 완료될 때까지 대기 (최대 3초)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<Trade> trades = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
                    return !trades.isEmpty();
                });

        List<Trade> byBuyUserIdAndTicker = tradeQueryUseCase.findByBuyUserIdAndTicker(1, ticker);
        List<Trade> bySellUserIdAndTicker = tradeQueryUseCase.findBySellUserIdAndTicker(2, ticker);
        assertNotNull(byBuyUserIdAndTicker, "매수인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, byBuyUserIdAndTicker.size(), "매수인의 거래 내역이 정확히 1개여야 합니다");

        assertNotNull(bySellUserIdAndTicker, "매도인의 거래 내역이 null이면 안됩니다");
        assertEquals(1, bySellUserIdAndTicker.size(), "매도인의 거래 내역이 정확히 1개여야 합니다");

        assertEquals(byBuyUserIdAndTicker.getFirst().getId(),
                bySellUserIdAndTicker.getFirst().getId(),
                "매수인와 매도인의 거래 내역은 동일한 거래를 가리켜야 합니다"
        );

        assertTrue(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT).isEmpty(), "지정가 매수 주문이 1개 남아있어야 합니다.");
        assertEquals(1, waitingOrders.getSellOrderPriorityQueueStore(OrderType.MARKET).size(), "남은 시장가 매도 주문이 없어야 합니다.");
    }

}
