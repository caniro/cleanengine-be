package com.cleanengine.coin.mypage.repository;

import com.cleanengine.coin.user.domain.User;
import com.cleanengine.coin.user.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MyRankingRepository extends JpaRepository<Wallet,Integer> {
    /**
     *개인별 랭킹을 위한 repo
     *
     **/
    List<Wallet> findAllByAccountId(Integer accountId);
}
