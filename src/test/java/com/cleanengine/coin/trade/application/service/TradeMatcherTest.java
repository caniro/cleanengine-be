package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.common.domain.port.PriorityQueueStore;
import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.Order;
import com.cleanengine.coin.order.domain.OrderType;
import com.cleanengine.coin.order.domain.SellOrder;
import com.cleanengine.coin.order.domain.spi.WaitingOrders;
import com.cleanengine.coin.trade.domain.model.TradePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TradeMatcher 단위 테스트")
@ExtendWith(MockitoExtension.class)
class TradeMatcherTest {

    @Mock
    private WaitingOrders waitingOrders;

    @Mock
    private PriorityQueueStore<SellOrder> marketSellQueueStore;

    @Mock
    private PriorityQueueStore<SellOrder> limitSellQueueStore;

    @Mock
    private PriorityQueueStore<BuyOrder> marketBuyQueueStore;

    @Mock
    private PriorityQueueStore<BuyOrder> limitBuyQueueStore;

    @Mock
    private SellOrder marketSellOrder;

    @Mock
    private SellOrder limitSellOrder;

    @Mock
    private BuyOrder marketBuyOrder;

    @Mock
    private BuyOrder limitBuyOrder;

    private TradeMatcher tradeMatcher;

    @BeforeEach
    void setUp() {
        when(waitingOrders.getSellOrderPriorityQueueStore(OrderType.MARKET)).thenReturn(marketSellQueueStore);
        when(waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT)).thenReturn(limitSellQueueStore);
        when(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.MARKET)).thenReturn(marketBuyQueueStore);
        when(waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT)).thenReturn(limitBuyQueueStore);

        tradeMatcher = new TradeMatcher();
    }

    @Test
    @DisplayName("시장가 매도, 지정가 매수가 정상적으로 매칭된다.")
    void testMatchMarketSellOrderWithLimitBuyOrder() {
        // given
        when(marketSellQueueStore.peek()).thenReturn(marketSellOrder);
        when(limitSellQueueStore.peek()).thenReturn(null);
        when(limitBuyQueueStore.peek()).thenReturn(limitBuyOrder);
        when(marketBuyQueueStore.peek()).thenReturn(null);

        // when
        Optional<TradePair<Order, Order>> result = tradeMatcher.matchOrders(waitingOrders);

        // then
        assertTrue(result.isPresent());
        assertThat(result.get().getSellOrder()).isEqualTo(marketSellOrder);
        assertThat(result.get().getBuyOrder()).isEqualTo(limitBuyOrder);
        verify(waitingOrders).getSellOrderPriorityQueueStore(OrderType.MARKET);
        verify(waitingOrders).getBuyOrderPriorityQueueStore(OrderType.LIMIT);
    }

    @Test
    @DisplayName("지정가 매도, 시장가 매수가 정상적으로 매칭된다.")
    void testMatchMarketBuyOrderWithLimitSellOrder() {
        // given
        when(marketSellQueueStore.peek()).thenReturn(null);
        when(limitSellQueueStore.peek()).thenReturn(limitSellOrder);
        when(limitBuyQueueStore.peek()).thenReturn(null);
        when(marketBuyQueueStore.peek()).thenReturn(marketBuyOrder);

        // when
        Optional<TradePair<Order, Order>> result = tradeMatcher.matchOrders(waitingOrders);

        // then
        assertTrue(result.isPresent());
        assertThat(result.get().getSellOrder()).isEqualTo(limitSellOrder);
        assertThat(result.get().getBuyOrder()).isEqualTo(marketBuyOrder);
        verify(waitingOrders).getBuyOrderPriorityQueueStore(OrderType.MARKET);
        verify(waitingOrders).getSellOrderPriorityQueueStore(OrderType.LIMIT);
    }

    @Test
    @DisplayName("지정가 매도, 지정가 매수가 정상적으로 매칭된다.")
    void testMatchLimitBuyOrderWithLimitSellOrder() {
        // given
        when(limitBuyOrder.getPrice()).thenReturn(100.0);
        when(limitSellOrder.getPrice()).thenReturn(90.0);

        when(limitBuyQueueStore.peek()).thenReturn(limitBuyOrder);
        when(limitSellQueueStore.peek()).thenReturn(limitSellOrder);
        when(marketSellQueueStore.peek()).thenReturn(null);
        when(marketBuyQueueStore.peek()).thenReturn(null);

        // when
        Optional<TradePair<Order, Order>> result = tradeMatcher.matchOrders(waitingOrders);

        // then
        assertTrue(result.isPresent());
        assertThat(result.get().getSellOrder()).isEqualTo(limitSellOrder);
        assertThat(result.get().getBuyOrder()).isEqualTo(limitBuyOrder);
        verify(waitingOrders).getBuyOrderPriorityQueueStore(OrderType.LIMIT);
        verify(waitingOrders).getSellOrderPriorityQueueStore(OrderType.LIMIT);
    }

    @Test
    @DisplayName("peek의 결과가 없을 경우 빈 Optional을 반환한다.")
    void testNoOrdersMatched() {
        // given
        when(marketSellQueueStore.peek()).thenReturn(null);
        when(limitSellQueueStore.peek()).thenReturn(null);
        when(limitBuyQueueStore.peek()).thenReturn(null);
        when(marketBuyQueueStore.peek()).thenReturn(null);

        // when
        Optional<TradePair<Order, Order>> result = tradeMatcher.matchOrders(waitingOrders);

        // then
        assertTrue(result.isEmpty());
        verify(waitingOrders).getSellOrderPriorityQueueStore(OrderType.MARKET);
        verify(waitingOrders).getSellOrderPriorityQueueStore(OrderType.LIMIT);
        verify(waitingOrders).getBuyOrderPriorityQueueStore(OrderType.MARKET);
        verify(waitingOrders).getBuyOrderPriorityQueueStore(OrderType.LIMIT);
    }

}
