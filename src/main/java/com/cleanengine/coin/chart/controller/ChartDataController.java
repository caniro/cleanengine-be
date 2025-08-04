package com.cleanengine.coin.chart.controller;


import com.cleanengine.coin.chart.dto.RealTimeOhlcDto;
import com.cleanengine.coin.chart.service.ChartSubscriptionService;
import com.cleanengine.coin.chart.service.RealTimeOhlcService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChartDataController {

    private final ChartSubscriptionService subscriptionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RealTimeOhlcService realTimeOhlcService;
    // 티커별 마지막으로 전송한 OHLC 데이터 캐싱
    @Getter
    private final Map<String, RealTimeOhlcDto> lastSentOhlcDataMap = new ConcurrentHashMap<>();


    /**
     * 1초마다 실행 - 실시간 OHLC 데이터 전송
     */
//    @Scheduled(fixedRate = 1000)
    public void publishRealTimeOhlc() {
        try {
            log.debug("△ 실시간 OHLC 데이터 스케줄러 실행");

            if (subscriptionService.getAllRealTimeOhlcSubscribedTickers().isEmpty()) {
                log.debug("실시간 OHLC 구독된 티커 없음, 전송 생략");
                return;
            }
            final LocalDateTime now = LocalDateTime.now();

            for (String ticker : subscriptionService.getAllRealTimeOhlcSubscribedTickers()) {
                try {
                    log.debug("티커 {} 실시간 OHLC 데이터 전송 중...", ticker);

                    RealTimeOhlcDto ohlcData = realTimeOhlcService.getAndUpdateCumulative1mOhlc(ticker, now);

                    if (ohlcData == null) {
                        RealTimeOhlcDto lastSentData = lastSentOhlcDataMap.get(ticker);
                        if (lastSentData != null) {
                            log.debug("티커 {}의 실시간 OHLC 데이터가 없습니다. 이전 데이터 재사용", ticker);
                            RealTimeOhlcDto updatedData = new RealTimeOhlcDto(lastSentData.getTicker(), now,
                                    lastSentData.getOpen(), lastSentData.getHigh(), lastSentData.getLow(), lastSentData.getClose(), lastSentData.getVolume());
                            messagingTemplate.convertAndSend("/topic/realTimeOhlc/" + ticker, updatedData);
                            lastSentOhlcDataMap.put(ticker, updatedData);
                        } else {
                            log.debug("티커 {}의 이전 OHLC 데이터도 없습니다. 빈 데이터 전송", ticker);
                            RealTimeOhlcDto emptyData = new RealTimeOhlcDto(ticker, now, 0.0, 0.0, 0.0, 0.0, 0.0);
                            messagingTemplate.convertAndSend("/topic/realTimeOhlc/" + ticker, emptyData);
                            lastSentOhlcDataMap.put(ticker, emptyData);
                        }
                    } else {
                        messagingTemplate.convertAndSend("/topic/realTimeOhlc/" + ticker, ohlcData);
                        lastSentOhlcDataMap.put(ticker, ohlcData);
                        log.debug("실시간 OHLC 데이터 전송: {}", ohlcData);
                    }
                } catch (Exception e) {
                    log.error("티커 {} 실시간 OHLC 데이터 처리 중 오류: {}", ticker, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("△ 실시간 OHLC 데이터 발행 중 오류: {}", e.getMessage(), e);
        }
    }
}