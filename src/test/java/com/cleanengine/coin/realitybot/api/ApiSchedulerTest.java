package com.cleanengine.coin.realitybot.api;

import com.cleanengine.coin.order.adapter.out.persistentce.asset.AssetRepository;
import com.cleanengine.coin.order.domain.Asset;
import com.cleanengine.coin.realitybot.domain.APIVWAPState;
import com.cleanengine.coin.realitybot.domain.VWAPMetricsRecorder;
import com.cleanengine.coin.realitybot.dto.Ticks;
import com.cleanengine.coin.realitybot.parser.TickParser;
import com.cleanengine.coin.realitybot.service.OrderGenerateService;
import com.cleanengine.coin.realitybot.service.TickServiceManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Disabled
public class ApiSchedulerTest {

    @Spy
    @InjectMocks
    private ApiScheduler apiScheduler;

    @Mock
    private BithumbAPIClient apiClient;
    @Mock
    private TickParser tickParser;
    @Mock
    private OrderGenerateService orderGenerateService;
    @Mock
    private APIVWAPState apiVWAPState;
    @Mock
    private TickServiceManager tickServiceManager;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Timer timer;
    @Mock
    private VWAPMetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        when(meterRegistry.timer(anyString())).thenReturn(timer);
        // Timer의 record 메서드가 람다를 실행하도록 설정
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(timer).record(any(Runnable.class));
    }

    @Test
    void marketAllRequestCallsAllTickers() throws InterruptedException {
        // Given
        List<Asset> assets = List.of(
                new Asset("BTC", "비트코인", null),
                new Asset("TRUMP", "트럼프", null),
                new Asset("ETH", "이더리움", null),
                new Asset("DOGE", "도지코인", null),
                new Asset("USDT", "테더", null),
                new Asset("PEPE", "페페", null),
                new Asset("XRP", "리플", null),
                new Asset("SOL", "솔라나", null),
                new Asset("SUI", "수이", null),
                new Asset("WLD", "월드코인", null)
        );
        List<Ticks> testTicks = List.of(
                new Ticks("BTC", "2025-06-01", "11:30:25", "2025-06-01T11:30:25.123Z", 95730000.0f, 0.0082, 95000000.0f, 730000.0, "ASK", 100001L),
                new Ticks("ETH", "2025-06-01", "11:31:10", "2025-06-01T11:31:10.456Z", 4850000.0f, 1.25, 4800000.0f, 50000.0, "BID", 100002L),
                new Ticks("DOGE", "2025-06-01", "11:32:45", "2025-06-01T11:32:45.789Z", 185.5f, 9500.0, 180.0f, 5.5, "ASK", 100003L)
        );

        when(assetRepository.findAll()).thenReturn(assets);
        when(apiClient.get(anyString())).thenReturn("[{data:...}]");
        when(tickParser.parseGson(anyString())).thenReturn(testTicks);
        when(tickServiceManager.getService(anyString())).thenReturn(apiVWAPState);
        doNothing().when(apiVWAPState).addTick(any());
        doNothing().when(recorder).recordApiVwap(anyString(),anyDouble());

        // When
        apiScheduler.MarketAllRequest();

        // Then
        verify(apiScheduler, times(assets.size())).MarketDataRequest(anyString());
        verify(orderGenerateService,times(assets.size())).generateOrder(anyString(),anyDouble(),anyDouble());
    }
}
