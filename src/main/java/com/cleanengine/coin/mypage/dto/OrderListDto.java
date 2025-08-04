package com.cleanengine.coin.mypage.dto;

import com.cleanengine.coin.order.OrderSide;
import com.cleanengine.coin.order.domain.OrderStatus;
import com.cleanengine.coin.order.domain.OrderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderListDto {
    private OrderSide side;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private Long orderId;
    private String ticker;
    private String name;
    private Double price;
    private Double orderSize;
    private Double remainingSize;
    private Double displaySize;
    private LocalDateTime tradeTime;
}
