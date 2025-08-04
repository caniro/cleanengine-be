package com.cleanengine.coin.mypage.repository;

import com.cleanengine.coin.order.domain.BuyOrder;
import com.cleanengine.coin.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyOrderListRepository extends JpaRepository<BuyOrder, Long> {
    Page<BuyOrder> findByUserId(Integer userId, Pageable pageable);
    long countByUserId(Integer userId);

    Page<BuyOrder> findByUserIdAndState(Integer userId, OrderStatus state, Pageable pageable);
    long countByUserIdAndState(Integer userId, OrderStatus state);
}
