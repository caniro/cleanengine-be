package com.cleanengine.coin.trade.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Integer id;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(name = "trade_time", nullable = false, updatable = false)
    private LocalDateTime tradeTime;

    @Column(name = "buy_user_id", nullable = false)
    private Integer buyUserId;

    @Column(name = "sell_user_id", nullable = false)
    private Integer sellUserId;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "size", nullable = false)
    private Double size;

    public Trade(String ticker, LocalDateTime tradeTime, Integer buyUserId, Integer sellUserId, Double price, Double size) {
        this.ticker = ticker;
        this.tradeTime = tradeTime;
        this.buyUserId = buyUserId;
        this.sellUserId = sellUserId;
        this.price = price;
        this.size = size;
    }

    public static Trade of(String ticker, LocalDateTime tradeTime, Integer buyUserId, Integer sellUserId, Double price, Double size) {
        return new Trade(ticker, tradeTime, buyUserId, sellUserId, price, size);
    }

}
