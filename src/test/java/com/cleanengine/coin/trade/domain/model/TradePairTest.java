package com.cleanengine.coin.trade.domain.model;

import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.Order;
import com.cleanengine.coin.order.domain.SellOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradePairTest {

    @DisplayName("매수, 매도 주문 1쌍을 체결쌍으로 지정한다.")
    @Test
    void newTradePair() {
        // given
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, "BTC", 3, 1.0, 1000.0, LocalDateTime.now(), false);
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(2L, "BTC", 4, 1.0, 1000.0, LocalDateTime.now(), false);

        // when
        TradePair<Order, Order> tradePair = TradePair.of(buyOrder, sellOrder);

        // then
        assertThat(tradePair).isNotNull();
        assertThat(tradePair.getBuyOrder()).isEqualTo(buyOrder);
        assertThat(tradePair.getSellOrder()).isEqualTo(sellOrder);
    }

    @DisplayName("매도 주문 2개로 체결쌍을 지정하면 예외가 발생한다.")
    @Test
    void newTradePairWIthTwoSellOrders() {
        // given
        SellOrder sellOrder1 = SellOrder.createLimitSellOrder(1L, "BTC", 3, 1.0, 1000.0, LocalDateTime.now(), false);
        SellOrder sellOrder2 = SellOrder.createLimitSellOrder(2L, "BTC", 5, 1.0, 1000.0, LocalDateTime.now(), false);

        // when, then
        assertThatThrownBy(() -> TradePair.of(sellOrder1, sellOrder2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("매수 주문과 매도 주문이 각각 하나씩 매칭되어야 합니다.");
    }

    @DisplayName("매수 주문 2개로 체결쌍을 지정하면 예외가 발생한다.")
    @Test
    void newTradePairWIthTwoBuyOrders() {
        // given
        BuyOrder buyOrder1 = BuyOrder.createLimitBuyOrder(1L, "BTC", 4, 1.0, 1000.0, LocalDateTime.now(), false);
        BuyOrder buyOrder2 = BuyOrder.createLimitBuyOrder(2L, "BTC", 6, 1.0, 1000.0, LocalDateTime.now(), false);

        // when, then
        assertThatThrownBy(() -> TradePair.of(buyOrder1, buyOrder2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("매수 주문과 매도 주문이 각각 하나씩 매칭되어야 합니다.");
    }

}
