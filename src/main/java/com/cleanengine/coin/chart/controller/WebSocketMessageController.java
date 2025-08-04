package com.cleanengine.coin.chart.controller;

import com.cleanengine.coin.chart.dto.RealTimeOhlcDto;
import com.cleanengine.coin.chart.service.ChartSubscriptionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageController {

    private final ChartSubscriptionService subscriptionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChartDataController chartDataController; // 이미 계산된 데이터를 가진 컨트롤러를 활용

    /**
     * 실시간 OHLC 데이터 구독 처리
     */
    @MessageMapping("/subscribe/realTimeOhlc")
    public void subscribeRealTimeOhlc(RealTimeTradeMappingDto request) {
        String ticker = request.getTicker();
        log.debug("실시간 OHLC 데이터 구독 요청: {}", ticker);

        // 구독 목록에 추가
        subscriptionService.subscribeRealTimeOhlc(ticker);

        RealTimeOhlcDto lastSentData = chartDataController.getLastSentOhlcDataMap().get(ticker);

        if (lastSentData != null) {
            // 캐시된 데이터가 있으면 즉시 전송
            log.debug("티커 {}의 캐시된 OHLC 데이터 즉시 전송: {}", ticker, lastSentData);
            messagingTemplate.convertAndSend("/topic/realTimeOhlc/" + ticker, lastSentData);
        }
    }

    /**
     * WebSocket 매핑용 DTO
     */
    @Setter
    @Getter
    public static class RealTimeTradeMappingDto {
        private String ticker;
    }
}