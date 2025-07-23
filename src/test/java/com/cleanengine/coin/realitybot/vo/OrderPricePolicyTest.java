package com.cleanengine.coin.realitybot.vo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class OrderPricePolicyTest {

    private final OrderPricePolicy policy = new OrderPricePolicy();

    @Test
    @DisplayName("Level 1: 매수는 VWAP 이상, 매도는 VWAP 이하이어야 함")
    void testLevel1() {
        double platformVWAP = 10000.0;
        double unitPrice = 10.0;
        double trendLineRate = 0.03;

        OrderPricePolicy.OrderPrice price = policy.calculatePrice(1, platformVWAP, unitPrice, trendLineRate);

        assertTrue(price.buy() > platformVWAP, "Level 1: 매수 가격은 VWAP보다 커야 함");
        assertTrue(price.sell() < platformVWAP, "Level 1: 매도 가격은 VWAP보다 작아야 함");
        assertEquals(0, price.buy() % unitPrice, "호가 단위로 정규화되어야 함");
        assertEquals(0, price.sell() % unitPrice, "호가 단위로 정규화되어야 함");
    }

    @Test
    @DisplayName("Level 5: 가격 차이가 충분히 커야 함")
    void testLevel5Spread() {
        int level = 5;
        double platformVWAP = 30000.0;
        double unitPrice = 100.0;
        double trendLineRate = 0.0;

        OrderPricePolicy.OrderPrice price = policy.calculatePrice(level, platformVWAP, unitPrice, trendLineRate);

        double minExpectedDiff = unitPrice * 5 * 0.8; // 랜덤 보정 고려해도 80% 이상 차이 기대
        assertTrue(price.sell() - price.buy() >= minExpectedDiff,
                "레벨 5는 충분한 가격 차이를 가져야 함");
    }

    @RepeatedTest(5)
    @DisplayName("Level 2~3: 가격 차이는 허용 범위 내, 호가 단위로 정규화됨")
    void testLevel2To3_priceDiffWithinRange() {
        for (int level = 2; level <= 3; level++) {
            double platformVWAP = 20000.0;
            double unitPrice = 50.0;
            double trendLineRate = -0.02;

            OrderPricePolicy.OrderPrice price = policy.calculatePrice(level, platformVWAP, unitPrice, trendLineRate);

            double priceDiff = price.sell() - price.buy();
            double priceOffset = unitPrice * level;
            double maxRandomOffset = platformVWAP * 0.01;
            double maxAllowedDiff = (priceOffset + maxRandomOffset) * 2;

            System.out.printf("level=%d, sell=%.1f, buy=%.1f, diff=%.1f, maxAllowed=%.1f%n",
                    level, price.sell(), price.buy(), priceDiff, maxAllowedDiff);

            assertTrue(Math.abs(priceDiff) <= maxAllowedDiff,
                    String.format("가격 차이 %.1f 이 최대 허용 범위 %.1f 초과", priceDiff, maxAllowedDiff));
            assertEquals(0, price.sell() % unitPrice, "매도 가격은 호가 단위여야 함");
            assertEquals(0, price.buy() % unitPrice, "매수 가격은 호가 단위여야 함");
        }
    }

}
