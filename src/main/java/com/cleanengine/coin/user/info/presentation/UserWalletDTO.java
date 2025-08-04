package com.cleanengine.coin.user.info.presentation;

import com.cleanengine.coin.user.info.application.DecimalPlaces2Serializer;
import com.cleanengine.coin.user.info.application.DecimalPlaces8Serializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserWalletDTO {

    @Schema(description = "종목 티커", example = "BTC")
    private final String ticker;

    @Schema(description = "종목명", example = "비트코인")
    private final String name;

    @Schema(description = "계좌 ID", example = "3")
    private final Integer accountId;

    @Schema(description = "보유수량", example = "2.5")
    @JsonSerialize(using = DecimalPlaces8Serializer.class)
    private final Double size;

    @Schema(description = "1주 평균 매수 금액", example = "15000")
    @JsonSerialize(using = DecimalPlaces8Serializer.class)
    private final Double buyPrice;  // 매수 평단

    @Schema(description = "수익률", example = "10")
    @JsonSerialize(using = DecimalPlaces2Serializer.class)
    private final Double roi;  // 수익률

    @Schema(description = "현재가(최근 체결가)", example = "16500")
    @JsonSerialize(using = DecimalPlaces8Serializer.class)
    private final Double currentPrice;  // 현재가(최근 체결가)

    @Builder
    private UserWalletDTO(String ticker, String name, Integer accountId, Double size, Double buyPrice, Double roi, Double currentPrice) {
        this.ticker = ticker;
        this.name = name;
        this.accountId = accountId;
        this.size = size;
        this.buyPrice = buyPrice;
        this.roi = roi;
        this.currentPrice = currentPrice;
    }

    public static UserWalletDTO of(String ticker, String name, Integer accountId, Double size, Double buyPrice, Double roi, Double currentPrice) {
        return UserWalletDTO.builder()
                .ticker(ticker)
                .name(name)
                .accountId(accountId)
                .size(size)
                .buyPrice(buyPrice)
                .roi(roi)
                .currentPrice(currentPrice)
                .build();
    }

}
