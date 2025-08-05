package com.cleanengine.coin.trade.application.service;

import com.cleanengine.coin.common.domain.port.PriorityQueueStore;
import com.cleanengine.coin.order.adapter.out.persistentce.order.command.BuyOrderRepository;
import com.cleanengine.coin.order.adapter.out.persistentce.order.command.SellOrderRepository;
import com.cleanengine.coin.order.application.event.OrderInsertedToQueue;
import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.OrderType;
import com.cleanengine.coin.order.domain.SellOrder;
import com.cleanengine.coin.order.domain.spi.WaitingOrders;
import com.cleanengine.coin.order.domain.spi.WaitingOrdersManager;
import com.cleanengine.coin.trade.application.port.out.TradeCommandRepository;
import com.cleanengine.coin.trade.application.port.out.TradeQueryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"dev", "it", "mariadb-local", "trade-load-test", "actuator", "apm", "otel-local"})
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
class TradeExecuteLoadTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    WaitingOrdersManager waitingOrdersManager;

    @Autowired
    TradeQueryRepository tradeQueryRepository;

    @Autowired
    TradeCommandRepository tradeCommandRepository;

    @Autowired
    private TradeFlowService tradeFlowService;

    @Autowired
    SellOrderRepository sellOrderRepository;

    @Autowired
    BuyOrderRepository buyOrderRepository;

    private final String ticker = "BTC";

    @DisplayName("워밍업: Spring 컨텍스트 및 JVM 최적화")
    @Order(1)
    @Test
//    @Disabled
    void warmUp() throws InterruptedException {
        System.out.println("Starting warmUp");
        int warmUpCount1 = 10;
        for (int i = 0; i < warmUpCount1; i++) {
            runSingleTest(1000);
        }
//        int warmUpCount2 = 5;
//        for (int i = 0; i < warmUpCount2; i++) {
//            runSingleTest(10000);
//        }
//        int warmUpCount3 = 5;
//        for (int i = 0; i < warmUpCount3; i++) {
//            runSingleTest(100000);
//        }
        System.out.println("Finished warmUp");
    }

    @BeforeEach
    void setUp() {
        WaitingOrders waitingOrders = waitingOrdersManager.getWaitingOrders(ticker);
        waitingOrders.clearAllQueues();
        tradeCommandRepository.deleteAll();
        sellOrderRepository.deleteAll();
        buyOrderRepository.deleteAll();
        tradeFlowService.setTestLatch(null);
    }

    @DisplayName("매수, 매도 각 1000건에 대한 처리 성능을 10회 진행한다.")
    @Order(2)
    @Test
//    @Disabled
    void basicLoadTestWith1000OrdersEachSide() throws InterruptedException {
        // given
        int orderCount = 1000;
        int repeatCount = 10;
        List<Long> executionTimes = new ArrayList<>();
        List<Long> queueInsertTimes = new ArrayList<>();

        // when
        for (int i = 0; i < repeatCount; ++i) {
            long[] times = runSingleTest(orderCount);
            queueInsertTimes.add(times[0]);
            executionTimes.add(times[1]);
            System.out.printf("Run-%d: 큐 삽입 소요시간 = %d ms, 체결 소요시간 = %d ms%n", (i + 1), times[0], times[1]);
        }

        // 통계 출력
        printStatistics(queueInsertTimes, executionTimes, orderCount);

        WaitingOrders waitingOrders = waitingOrdersManager.getWaitingOrders(ticker);
        PriorityQueueStore<BuyOrder> buyOrderPriorityQueueStore = waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT);
        PriorityQueueStore<SellOrder> sellOrderPriorityQueueStore = waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT);

        assertThat(sellOrderPriorityQueueStore.size()).isEqualTo(0);
        assertThat(buyOrderPriorityQueueStore.size()).isEqualTo(0);
    }

    @DisplayName("매수, 매도 각 10000건에 대한 처리 성능을 10회 진행한다.")
    @Order(3)
    @Test
    @Disabled
    void basicLoadTestWith10000OrdersEachSide() throws InterruptedException {
        // given
        int orderCount = 10000;
        int repeatCount = 10;
        List<Long> executionTimes = new ArrayList<>();
        List<Long> queueInsertTimes = new ArrayList<>();

        // when
        for (int i = 0; i < repeatCount; ++i) {
            long[] times = runSingleTest(orderCount);
            queueInsertTimes.add(times[0]);
            executionTimes.add(times[1]);
            System.out.printf("Run-%d: 큐 삽입 소요시간 = %d ms, 체결 소요시간 = %d ms%n", (i + 1), times[0], times[1]);
        }

        // 통계 출력
        printStatistics(queueInsertTimes, executionTimes, orderCount);

        WaitingOrders waitingOrders = waitingOrdersManager.getWaitingOrders(ticker);
        PriorityQueueStore<BuyOrder> buyOrderPriorityQueueStore = waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT);
        PriorityQueueStore<SellOrder> sellOrderPriorityQueueStore = waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT);

        assertThat(sellOrderPriorityQueueStore.size()).isEqualTo(0);
        assertThat(buyOrderPriorityQueueStore.size()).isEqualTo(0);
    }

    @DisplayName("매수, 매도 각 100000건에 대한 처리 성능을 10회 진행한다.")
    @Order(4)
    @Test
    @Disabled
    void basicLoadTestWith100000OrdersEachSide() throws InterruptedException {
        // given
        int orderCount = 100000;
        int repeatCount = 10;
        List<Long> executionTimes = new ArrayList<>();
        List<Long> queueInsertTimes = new ArrayList<>();

        // when
        for (int i = 0; i < repeatCount; ++i) {
            long[] times = runSingleTest(orderCount);
            queueInsertTimes.add(times[0]);
            executionTimes.add(times[1]);
            System.out.printf("Run-%d: 큐 삽입 소요시간 = %d ms, 체결 소요시간 = %d ms%n", (i + 1), times[0], times[1]);
        }

        // 통계 출력
        printStatistics(queueInsertTimes, executionTimes, orderCount);
    }

    private long[] runSingleTest(int orderCount) throws InterruptedException {
        WaitingOrders waitingOrders = waitingOrdersManager.getWaitingOrders(ticker);

        // 큐와 DB 초기화
        waitingOrders.clearAllQueues();
        tradeCommandRepository.deleteAll();
        sellOrderRepository.deleteAll();
        buyOrderRepository.deleteAll();

        CountDownLatch latch = new CountDownLatch(1);
        tradeFlowService.setTestLatch(latch);

        long testStart = System.nanoTime();

        // 주문 생성 및 큐 삽입
        final LocalDateTime baseTime = LocalDateTime.now();

        for (int i = 0; i < orderCount; i++) {
            final LocalDateTime orderTime = baseTime.plusSeconds(i);

            SellOrder limitSellOrder = SellOrder.createLimitSellOrder(1L, ticker, 1, 10.0, 130_000_000.0, orderTime, true);
            BuyOrder limitBuyOrder = BuyOrder.createLimitBuyOrder(2L, ticker, 2, 10.0, 130_000_000.0, orderTime, true);
            waitingOrders.addOrder(limitSellOrder);
            waitingOrders.addOrder(limitBuyOrder);

            sellOrderRepository.save(limitSellOrder);
            buyOrderRepository.save(limitBuyOrder);
        }

        PriorityQueueStore<BuyOrder> buyOrderPriorityQueueStore = waitingOrders.getBuyOrderPriorityQueueStore(OrderType.LIMIT);
        PriorityQueueStore<SellOrder> sellOrderPriorityQueueStore = waitingOrders.getSellOrderPriorityQueueStore(OrderType.LIMIT);

        long queueInsertEnd = System.nanoTime();
        long queueInsertTime = (queueInsertEnd - testStart) / 1_000_000;

        // 단일 이벤트 발행 (체결 시작, 큐에는 안 넣음)
        long eventStart = System.nanoTime();
        SellOrder dummyOrder = SellOrder.createLimitSellOrder(1L, ticker, 1, 10.0, 130_000_000.0, LocalDateTime.now(), true);
        eventPublisher.publishEvent(new OrderInsertedToQueue(dummyOrder));


        boolean completed = latch.await(2, TimeUnit.MINUTES);
        long eventEnd = System.nanoTime();
        long executionTime = (eventEnd - eventStart) / 1_000_000;

        // CountDownLatch 초기화
        tradeFlowService.setTestLatch(null);

        // 결과 출력
        long tradeCount = tradeQueryRepository.count();
        System.out.print("체결 종료 - 체결내역[: " + tradeCount + "건]");
        System.out.println("잔여 주문[매도 " + sellOrderPriorityQueueStore.size() + "건, 매수 " + buyOrderPriorityQueueStore.size() + "건]");
        if (tradeCount != orderCount || !completed) {
            System.out.println("경고: 예상 체결 건수(" + orderCount + "건)와 실제(" + tradeCount + "건) 불일치 또는 타임아웃");
        }

        return new long[]{queueInsertTime, executionTime};
    }

    private void printStatistics(List<Long> queueInsertTimes, List<Long> executionTimes, int orderCount) {
        // 큐 삽입 시간 통계
        double queueAvg = queueInsertTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long queueMin = queueInsertTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long queueMax = queueInsertTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double queueStdDev = calculateStdDev(queueInsertTimes, queueAvg);

        // 체결 시간 통계
        double executionAvg = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long executionMin = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long executionMax = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double executionStdDev = calculateStdDev(executionTimes, executionAvg);

        // 처리량 통계 (체결 시간 기반)
        double throughputAvg = (orderCount * 2) / (executionAvg / 1000.0);
        double throughputMin = (orderCount * 2) / (executionMax / 1000.0);
        double throughputMax = (orderCount * 2) / (executionMin / 1000.0);

        System.out.printf("=== 통계 결과 ===%n");
        System.out.printf("큐 삽입 시간 - 평균: %.2f ms, 최소: %d ms, 최대: %d ms, 표준편차: %.2f ms%n",
                queueAvg, queueMin, queueMax, queueStdDev);
        System.out.printf("체결 시간 - 평균: %.2f ms, 최소: %d ms, 최대: %d ms, 표준편차: %.2f ms%n",
                executionAvg, executionMin, executionMax, executionStdDev);
        System.out.printf("처리량 - 평균: %.2f orders/sec, 최소: %.2f orders/sec, 최대: %.2f orders/sec%n",
                throughputAvg, throughputMin, throughputMax);
    }

    private double calculateStdDev(List<Long> times, double mean) {
        double sum = times.stream().mapToDouble(t -> Math.pow(t - mean, 2)).sum();
        return Math.sqrt(sum / times.size());
    }

}
