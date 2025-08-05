package com.cleanengine.coin.trade.adapter.out.event;

import com.cleanengine.coin.trade.domain.event.TradeExecutedEvent;
import com.cleanengine.coin.trade.domain.model.Trade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringTradeExecutedEventPublisherTest {

    @Mock
    private ApplicationEventPublisher mockPublisher;

    @InjectMocks
    private SpringTradeExecutedEventPublisher publisher;

    @DisplayName("체결 완료 이벤트를 정상적으로 발행한다.")
    @Test
    void simplePublish() {
        // given
        Trade newTrade = Trade.of("BTC", LocalDateTime.now(), 2, 1, 1000.0, 10.0);
        TradeExecutedEvent tradeExecutedEvent = TradeExecutedEvent.of(newTrade, 1L, 2L);

        // when
        publisher.publish(tradeExecutedEvent);

        // then
        verify(mockPublisher, times(1))
                .publishEvent(tradeExecutedEvent);
    }

    @DisplayName("여러 이벤트를 정상적으로 발행한다.")
    @Test
    void publishMultipleEvents() {
        // given
        Trade trade1 = Trade.of("BTC", LocalDateTime.now(), 2, 1, 2000.0, 20.0);
        Trade trade2 = Trade.of("TRUMP", LocalDateTime.now(), 2, 1, 3000.0, 30.0);
        TradeExecutedEvent event1 = TradeExecutedEvent.of(trade1, 1L, 2L);
        TradeExecutedEvent event2 = TradeExecutedEvent.of(trade2, 3L, 4L);

        // when
        publisher.publish(event1);
        publisher.publish(event2);

        // then
        verify(mockPublisher, times(1)).publishEvent(event1);
        verify(mockPublisher, times(1)).publishEvent(event2);
    }

    @DisplayName("null 이벤트를 발행한 경우 예외가 발생한다.")
    @Test
    void publishNullEvent() {
        // given

        // when, then
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("event must not be null");
    }

    @DisplayName("체결 객체가 null인 이벤트를 발행한 경우 예외가 발생한다.")
    @Test
    void publishEventWithNullTrade() {
        // given
        TradeExecutedEvent eventWithNullTrade = TradeExecutedEvent.of(null, 1L, 2L);

        // when, then
        assertThatThrownBy(() -> publisher.publish(eventWithNullTrade))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("trade must not be null");
    }

    @DisplayName("buyOrderId가 null인 이벤트를 발행한 경우 예외가 발생한다.")
    @Test
    void publishEventWithNullBuyOrderId() {
        // given
        Trade trade = Trade.of("BTC", LocalDateTime.now(), 2, 1, 1000.0, 10.0);
        TradeExecutedEvent eventWithNullBuyOrderId = TradeExecutedEvent.of(trade, null, 2L);

        // when, then
        assertThatThrownBy(() -> publisher.publish(eventWithNullBuyOrderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("buyOrderId must not be null");
    }

    @DisplayName("sellOrderId가 null인 이벤트를 발행한 경우 예외가 발생한다.")
    @Test
    void publishEventWithNullSellOrderId() {
        // given
        Trade trade = Trade.of("BTC", LocalDateTime.now(), 2, 1, 1000.0, 10.0);
        TradeExecutedEvent eventWithNullSellOrderId = TradeExecutedEvent.of(trade, 1L, null);

        // when, then
        assertThatThrownBy(() -> publisher.publish(eventWithNullSellOrderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageMatching("sellOrderId must not be null");
    }

}
