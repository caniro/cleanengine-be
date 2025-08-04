package com.cleanengine.coin.order.application;

import com.cleanengine.coin.chart.repository.RealTimeTradeRepository;
import com.cleanengine.coin.order.application.dto.AssetInfo;
import com.cleanengine.coin.trade.entity.Trade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssetService H2 통합 테스트")
@ActiveProfiles({"dev", "it", "h2-mem"})
@Transactional
@SpringBootTest
class AssetServiceIntegrationTest {

    @Autowired
    AssetService assetService;

    @Autowired
    RealTimeTradeRepository realTimeTradeRepository;

    @DisplayName("모든 종목 정보를 조회한다.")
    @Test
    void getAllAssetInfos() {
        // given
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atStartOfDay().plusHours(12);
        LocalDateTime today = LocalDate.now().atStartOfDay().plusHours(12);
        Trade prevTrumpTrade = Trade.of("TRUMP", yesterday, 2, 1, 10000.0, 1.0);
        Trade todayTrumpTrade = Trade.of("TRUMP", today, 2, 1, 20000.0, 1.0);
        realTimeTradeRepository.saveAll(List.of(prevTrumpTrade, todayTrumpTrade));
        realTimeTradeRepository.flush();

        // when
        List<AssetInfo> allAssetInfos = assetService.getAllAssetInfos();
        AssetInfo trumpAsset = null;
        for (AssetInfo assetInfo : allAssetInfos) {
            if (assetInfo.ticker().equals("TRUMP")) trumpAsset = assetInfo;
        }

        // then
        assertThat(trumpAsset).isNotNull()
                .extracting("ticker", "changeRate", "currentPrice")
                .containsExactly("TRUMP", 100.0, 20000.0);

    }

    @DisplayName("전날 체결내역이 없는 경우 변동률 null을 반환한다.")
    @Test
    void getAllAssetInfosWithoutChangeRate() {
        // given
        LocalDateTime today = LocalDate.now().atStartOfDay().plusHours(12);
        Trade todayTrumpTrade = Trade.of("TRUMP", today, 2, 1, 20000.0, 1.0);
        realTimeTradeRepository.saveAll(List.of(todayTrumpTrade));
        realTimeTradeRepository.flush();

        // when
        List<AssetInfo> allAssetInfos = assetService.getAllAssetInfos();
        AssetInfo trumpAsset = null;
        for (AssetInfo assetInfo : allAssetInfos) {
            if (assetInfo.ticker().equals("TRUMP")) trumpAsset = assetInfo;
        }

        // then
        assertThat(trumpAsset).isNotNull()
                .extracting("ticker", "changeRate", "currentPrice")
                .containsExactly("TRUMP", null, 20000.0);
    }

}
