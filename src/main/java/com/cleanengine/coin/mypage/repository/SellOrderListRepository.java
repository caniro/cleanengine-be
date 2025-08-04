package com.cleanengine.coin.mypage.repository;

import com.cleanengine.coin.order.domain.OrderStatus;
import com.cleanengine.coin.order.domain.SellOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SellOrderListRepository extends JpaRepository<SellOrder, Integer> {
    Page<SellOrder> findByUserId(Integer userId, Pageable pageable);
    long countByUserId(Integer userId);

    Page<SellOrder> findByUserIdAndState(Integer userId, OrderStatus state, Pageable pageable);
    long countByUserIdAndState(Integer userId, OrderStatus state);
}
