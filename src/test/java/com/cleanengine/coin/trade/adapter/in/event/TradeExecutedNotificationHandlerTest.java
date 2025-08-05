package com.cleanengine.coin.trade.adapter.in.event;

import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.SellOrder;
import com.cleanengine.coin.trade.application.dto.TradeOrderCompletedNotifyDto;
import com.cleanengine.coin.trade.domain.event.TradeOrderCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static com.cleanengine.coin.common.CommonValues.BUY_ORDER_BOT_ID;
import static com.cleanengine.coin.common.CommonValues.SELL_ORDER_BOT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("체결 알림 단위테스트")
@ExtendWith(MockitoExtension.class)
class TradeExecutedNotificationHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TradeExecutedNotificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TradeExecutedNotificationHandler(messagingTemplate);
    }

    @DisplayName("매도인은 봇인 정상 체결내역을 리스닝하면 웹소켓으로 전송한다.")
    @Test
    void shouldSendNotificationsForValidTrade() {
        // given
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, "BTC", 3, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        TradeOrderCompletedEvent event = TradeOrderCompletedEvent.of(sellOrder);

        // when
        handler.notifyAfterTradeExecuted(event);

        // then
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/tradeNotification/3"), any(TradeOrderCompletedNotifyDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/tradeNotification/3"), any(TradeOrderCompletedNotifyDto.class));
    }

    @DisplayName("매수인은 봇인 정상 체결내역을 리스닝하면 웹소켓으로 전송한다.")
    @Test
    void shouldSendNotificationsForValidTrade2() {
        // given
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, "BTC", 4, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        TradeOrderCompletedEvent event = TradeOrderCompletedEvent.of(buyOrder);

        // when
        handler.notifyAfterTradeExecuted(event);

        // then
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/tradeNotification/4"), any(TradeOrderCompletedNotifyDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/tradeNotification/4"), any(TradeOrderCompletedNotifyDto.class));
    }

    @DisplayName("매수인의 userId가 null이면 메시지를 전송하지 않는다.")
    @Test
    void shouldNotSendNotificationForNullBuyUserId() {
        // given
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(1L, "BTC", null, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        TradeOrderCompletedEvent event = TradeOrderCompletedEvent.of(buyOrder);

        // when
        handler.notifyAfterTradeExecuted(event);

        // then
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("매도인의 userId가 null이면 메시지를 전송하지 않는다.")
    @Test
    void shouldNotSendNotificationForNullSellUserId() {
        // given
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, "BTC", null, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        TradeOrderCompletedEvent event = TradeOrderCompletedEvent.of(sellOrder);

        // when
        handler.notifyAfterTradeExecuted(event);

        // then
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("봇의 체결은 메시지를 전송하지 않는다.")
    @Test
    void shouldNotSendNotificationForBotTrade() {
        // given
        SellOrder sellOrder = SellOrder.createLimitSellOrder(1L, "BTC", SELL_ORDER_BOT_ID, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        BuyOrder buyOrder = BuyOrder.createLimitBuyOrder(2L, "BTC", BUY_ORDER_BOT_ID, 5.0, 130_000_000.0, LocalDateTime.now(), false);
        TradeOrderCompletedEvent event = TradeOrderCompletedEvent.of(sellOrder);
        TradeOrderCompletedEvent event2 = TradeOrderCompletedEvent.of(buyOrder);

        // when
        handler.notifyAfterTradeExecuted(event);
        handler.notifyAfterTradeExecuted(event2);

        // then
        verifyNoInteractions(messagingTemplate);
    }

    @DisplayName("주문이 null이면 메시지를 전송하지 않는다.")
    @Test
    void shouldNotSendNotificationForNullTrade() {
        // given
        TradeOrderCompletedEvent event = TradeOrderCompletedEvent.of(null);

        // when
        handler.notifyAfterTradeExecuted(event);

        // then
        verifyNoInteractions(messagingTemplate);
    }

}
