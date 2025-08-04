package com.cleanengine.coin.order.application.dto;

import com.cleanengine.coin.order.domain.Asset;
import com.cleanengine.coin.user.info.application.DecimalPlaces2Serializer;
import com.cleanengine.coin.user.info.application.DecimalPlaces8Serializer;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.StandardCharsets;

@JsonPropertyOrder({"ticker", "name"})
public record AssetInfo(

        @Schema(description = "종목 티커", example = "BTC")
        String ticker,

        @Schema(description = "종목명", example = "비트코인")
        String name,

        @Schema(description = "아이콘 SVG(base64)")
        String svgIconBase64,

        @Schema(description = "현재가(체결내역 없으면 null)", example = "161000000")
        @JsonSerialize(using = DecimalPlaces8Serializer.class)
        Double currentPrice,

        @Schema(description = "변동률(전일 체결내역 없으면 null)", example = "1.2")
        @JsonSerialize(using = DecimalPlaces2Serializer.class)
        Double changeRate

) {

    public static AssetInfo from(Asset asset, Double currentPrice, Double changeRate){
        byte[] iconBytes = asset.getIcon();

        String iconStr = null;

        if(iconBytes != null){
            iconStr = new String(iconBytes, StandardCharsets.UTF_8);
        }

        return new AssetInfo(asset.getTicker(), asset.getName(), iconStr, currentPrice, changeRate);
    }

}
