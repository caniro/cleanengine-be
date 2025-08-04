package com.cleanengine.coin.user.info.application;

import com.cleanengine.coin.order.adapter.out.persistentce.asset.AssetRepository;
import com.cleanengine.coin.user.domain.QAccount;
import com.cleanengine.coin.user.domain.QWallet;
import com.cleanengine.coin.user.domain.Wallet;
import com.cleanengine.coin.user.info.infra.AccountRepository;
import com.cleanengine.coin.user.info.infra.WalletRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    private final AccountRepository accountRepository;

    private final AssetRepository assetRepository;

    private final JPAQueryFactory queryFactory;

    private final EntityManager entityManager;

    public WalletService(WalletRepository walletRepository, AccountRepository accountRepository,
                         AssetRepository assetRepository, EntityManager entityManager, EntityManager entityManager1) {
        this.walletRepository = walletRepository;
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
        this.queryFactory = new JPAQueryFactory(entityManager);
        this.entityManager = entityManager1;
    }

    @Transactional
    public List<Wallet> findByAccountId(Integer accountId) {
        return walletRepository.findByAccountId(accountId);
    }

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    public List<Wallet> saveAll(List<Wallet> wallets) {
        return walletRepository.saveAll(wallets);
    }

    public Wallet findWalletByUserIdAndTicker(Integer userId, String ticker) {
        return walletRepository.findByUserIdAndTicker(userId, ticker)
                .orElseGet(() -> Wallet.of(ticker, accountRepository.findByUserId(userId).orElseThrow().getId()));
    }

    public List<Wallet> createNewWallets(Integer accountId) {
        List<Wallet> wallets = assetRepository.findAll()
                .stream()
                .map(asset -> Wallet.of(asset.getTicker(), accountId)).toList();
        
        walletRepository.saveAll(wallets);

        return wallets;
    }

    @Transactional
    public void updateWalletAfterTrade(int buyUserId, String ticker, double tradedPrice, double tradedSize) {
        QWallet wallet = QWallet.wallet;
        QAccount account = QAccount.account;

        long executedCount = queryFactory
                .update(wallet)
                .where(wallet.accountId.eq(
                        queryFactory
                                .select(account.id)
                                .from(account)
                                .where(account.userId.eq(buyUserId))
                ).and(wallet.ticker.eq(ticker)))
                .set(wallet.buyPrice,
                        wallet.buyPrice.coalesce(0.0)
                                .multiply(wallet.size)
                                .add(tradedPrice * tradedSize)
                                .divide(wallet.size.add(tradedSize))
                )
                .set(wallet.size, wallet.size.coalesce(0.0).add(tradedSize))
                .execute();

        if (executedCount > 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

}
