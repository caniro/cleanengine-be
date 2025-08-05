package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.trade.application.port.out.TradeQueryRepository;
import com.cleanengine.coin.trade.domain.model.Trade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeQueryService 단위 테스트")
class TradeQueryServiceTest {

    @Mock
    private TradeQueryRepository tradeQueryRepository;

    @InjectMocks
    private TradeQueryService tradeQueryService;

    @Test
    @DisplayName("성공적으로 거래 목록들을 반환한다.")
    void findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc_ReturnsTradesSuccessfully() {
        // given
        String ticker = "BTC";
        LocalDateTime startTime = LocalDateTime.now().minusDays(2);
        LocalDateTime endTime = LocalDateTime.now();
        Trade mockTrade = new Trade("BTC", endTime.minusDays(2), 2, 1, 50000.0, 0.01);
        Trade mockTrade2 = new Trade("BTC", endTime.minusDays(1), 3, 4, 55000.0, 1.0);
        Trade mockTrade3 = new Trade("BTC", endTime, 5, 6, 50000.0, 2.0);
        List<Trade> trades = List.of(mockTrade, mockTrade2, mockTrade3);
        when(tradeQueryRepository.findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(ticker, startTime, endTime))
                .thenReturn(trades);

        // when
        List<Trade> result = tradeQueryService.findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(ticker, startTime, endTime);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3);
        assertThat(result.getFirst().getTicker()).isEqualTo("BTC");
        assertThat(result.getFirst().getTradeTime()).isEqualTo(startTime);
        assertThat(result.getFirst().getBuyUserId()).isEqualTo(2);
    }

    @Test
    @DisplayName("티커와 시간 조건으로 거래 내역을 찾을 수 없는 경우 빈 거래 목록을 반환한다.")
    void findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc_ReturnsEmptyList() {
        // given
        String ticker = "BTC";
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();
        when(tradeQueryRepository.findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(ticker, startTime, endTime))
                .thenReturn(Collections.emptyList());

        // when
        List<Trade> result = tradeQueryService.findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(ticker, startTime, endTime);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("성공적으로 구매자 ID와 티커로 거래 목록을 반환한다.")
    void findByBuyUserIdAndTicker_ReturnsTradesSuccessfully() {
        // given
        Integer buyUserId = 2;
        String ticker = "BTC";
        Trade mockTrade = new Trade("BTC", LocalDateTime.now().minusDays(1), 2, 1, 50000.0, 0.5);
        Trade mockTrade2 = new Trade("BTC", LocalDateTime.now(), 2, 3, 60000.0, 1.0);
        List<Trade> trades = List.of(mockTrade, mockTrade2);
        when(tradeQueryRepository.findByBuyUserIdAndTicker(buyUserId, ticker)).thenReturn(trades);

        // when
        List<Trade> result = tradeQueryService.findByBuyUserIdAndTicker(buyUserId, ticker);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getBuyUserId()).isEqualTo(buyUserId);
        assertThat(result.getFirst().getTicker()).isEqualTo(ticker);
    }

    @Test
    @DisplayName("구매자 ID와 티커로 거래 목록을 찾지 못한 경우 빈 목록을 반환한다.")
    void findByBuyUserIdAndTicker_ReturnsEmptyList() {
        // given
        Integer buyUserId = 2;
        String ticker = "BTC";
        when(tradeQueryRepository.findByBuyUserIdAndTicker(buyUserId, ticker)).thenReturn(Collections.emptyList());

        // when
        List<Trade> result = tradeQueryService.findByBuyUserIdAndTicker(buyUserId, ticker);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("티커가 null인 경우 IllegalArgumentException을 발생시킨다.")
    void findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc_ThrowsIllegalArgumentException_ForNullArguments() {
        // given
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        // when, then
        assertThatThrownBy(() ->
                tradeQueryService.findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(null, startTime, endTime)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticker cannot be null");
    }

}
