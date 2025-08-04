package com.cleanengine.coin.mypage.service;

import com.cleanengine.coin.mypage.dto.OrderListDto;
import com.cleanengine.coin.mypage.dto.PagedOrderListDto;
import com.cleanengine.coin.mypage.repository.BuyOrderListRepository;
import com.cleanengine.coin.mypage.repository.SellOrderListRepository;
import com.cleanengine.coin.order.OrderSide;
import com.cleanengine.coin.order.adapter.out.persistentce.asset.AssetRepository;
import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.OrderStatus;
import com.cleanengine.coin.order.domain.OrderType;
import com.cleanengine.coin.order.domain.SellOrder;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class OrderListService {
    private final BuyOrderListRepository buyOrderListRepository;
    private final SellOrderListRepository sellOrderListRepository;
    private final AssetRepository assetRepository;

    public PagedOrderListDto getOrderList(Integer userId, int currentPage, int pageSize, boolean settled) {//todo cursortradetime, long cursorid, int pageisze 받기
        //커서기반에서 오프셋 기반으로 변경
        int adjustedPage = currentPage - 1;
        PageRequest pageRequest = PageRequest.of(adjustedPage, pageSize, Sort.by(Sort.Direction.DESC,"createdAt","id"));
        List<BuyOrder> buyOrders;
        List<SellOrder> sellOrders;
        long totalBuyOrders;
        long totalSellOrders;
        if (settled) {
            buyOrders = buyOrderListRepository.findByUserIdAndStateIn(userId,List.of(OrderStatus.DONE,OrderStatus.CANCELED),pageRequest).getContent();
            sellOrders = sellOrderListRepository.findByUserIdAndStateIn(userId,List.of(OrderStatus.DONE,OrderStatus.CANCELED),pageRequest).getContent();
            totalBuyOrders = buyOrderListRepository.countByUserIdAndStateIn(userId,List.of(OrderStatus.DONE,OrderStatus.CANCELED));
            totalSellOrders = sellOrderListRepository.countByUserIdAndStateIn(userId,List.of(OrderStatus.DONE,OrderStatus.CANCELED));
        }else {
            buyOrders = buyOrderListRepository.findByUserIdAndState(userId, OrderStatus.WAIT,pageRequest).getContent();
            sellOrders = sellOrderListRepository.findByUserIdAndState(userId,OrderStatus.WAIT,pageRequest).getContent();
            totalBuyOrders = buyOrderListRepository.countByUserIdAndState(userId,OrderStatus.WAIT);
            totalSellOrders = sellOrderListRepository.countByUserIdAndState(userId,OrderStatus.WAIT);

        }
        List<OrderListDto> buyDtos = buyOrders.stream()
                .map(b ->
                        new OrderListDto(OrderSide.BID,b.getState(),
                                convertToOrderType(b.getIsMarketOrder()),
                                b.getId(), b.getTicker(),
                        assetRepository.findByTicker(b.getTicker()).getName(),b.getPrice()
                                ,b.getOrderSize(),b.getRemainingSize(),
                                calcDisplaySize(b.getState(),b.getOrderSize(),b.getRemainingSize())
                                ,b.getCreatedAt())).toList();
        List<OrderListDto> sellDtos = sellOrders.stream()
                .map(s ->
                        new OrderListDto(OrderSide.ASK,s.getState(),
                                convertToOrderType(s.getIsMarketOrder()),
                                s.getId(), s.getTicker(),
                                assetRepository.findByTicker(s.getTicker()).getName(),s.getPrice()
                                ,s.getOrderSize(),s.getRemainingSize(),
                                calcDisplaySize(s.getState(),s.getOrderSize(),s.getRemainingSize())
                                ,s.getCreatedAt())).toList();
        List<OrderListDto> completedOrderDtos = Stream.concat(buyDtos.stream(),sellDtos.stream())
                .sorted(Comparator.comparing(OrderListDto::getTradeTime).reversed()).toList();
        long totalElements = totalBuyOrders + totalSellOrders;

        int totalPages = (int) Math.ceil((double)totalElements / pageSize);

        return new PagedOrderListDto(totalPages,totalElements,currentPage,pageSize,completedOrderDtos);
    }
        private double calcDisplaySize(OrderStatus orderStatus,Double orderSize,Double remainingSize) {
            if (orderStatus == null) return 0.0;

            return switch (orderStatus) {
                case DONE -> orderSize; //전체 수량
                case WAIT -> remainingSize; //남은 수량
                case CANCELED -> orderSize - remainingSize; //실제로 체결 된 수량
            };
        }
    private OrderType convertToOrderType(boolean isMarketOrder) {
            return isMarketOrder ? OrderType.MARKET : OrderType.LIMIT;
    }
}
