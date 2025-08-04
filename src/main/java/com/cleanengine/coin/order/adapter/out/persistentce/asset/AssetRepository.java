package com.cleanengine.coin.order.adapter.out.persistentce.asset;

import com.cleanengine.coin.order.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, String> {
    @Override
    List<Asset> findAll();

    @Query("SELECT a.name FROM Asset a WHERE a.ticker = :ticker")
    String findNameById(String ticker);

    Asset findByTicker(String ticker);
}
