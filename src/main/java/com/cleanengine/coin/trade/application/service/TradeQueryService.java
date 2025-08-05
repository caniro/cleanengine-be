package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.orderbook.dto.ClosingPriceDto;
import com.cleanengine.coin.trade.application.port.in.TradeQueryUseCase;
import com.cleanengine.coin.trade.application.port.out.TradeQueryRepository;
import com.cleanengine.coin.trade.domain.model.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class TradeQueryService implements TradeQueryUseCase {

    private final TradeQueryRepository tradeQueryRepository;

    @Override
    public List<Trade> findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(String ticker, LocalDateTime startTime, LocalDateTime endTime) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.findByTickerAndTradeTimeBetweenOrderByTradeTimeAsc(ticker, startTime, endTime);
    }

    @Override
    public List<Trade> findByBuyUserIdAndTicker(Integer buyUserId, String ticker) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.findByBuyUserIdAndTicker(buyUserId, ticker);
    }

    @Override
    public List<Trade> findBySellUserIdAndTicker(Integer sellUserId, String ticker) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.findBySellUserIdAndTicker(sellUserId, ticker);
    }

    @Override
    public List<Trade> findTop10ByTickerOrderByTradeTimeDesc(String ticker) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.findTop10ByTickerOrderByTradeTimeDesc(ticker);
    }

    @Override
    public List<Trade> findByTickerAndTradeTimeGreaterThanEqualOrderByTradeTimeDesc(String ticker, LocalDateTime lastTime) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.findByTickerAndTradeTimeGreaterThanEqualOrderByTradeTimeDesc(ticker, lastTime);
    }

    @Override
    public Trade findFirstByTickerOrderByTradeTimeDesc(String ticker) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.findFirstByTickerOrderByTradeTimeDesc(ticker);
    }

    @Override
    public ClosingPriceDto getYesterdayClosingPrice(String ticker, LocalDate yesterdayDate) {
        if (ticker == null)
            throw new IllegalArgumentException("Ticker cannot be null");
        return tradeQueryRepository.getYesterdayClosingPrice(ticker, yesterdayDate);
    }

    @Override
    public long count() {
        return tradeQueryRepository.count();
    }

}
