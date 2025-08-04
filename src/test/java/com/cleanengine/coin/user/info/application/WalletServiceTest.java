package com.cleanengine.coin.user.info.application;

import com.cleanengine.coin.user.domain.Account;
import com.cleanengine.coin.user.domain.Wallet;
import com.cleanengine.coin.user.info.infra.AccountRepository;
import com.cleanengine.coin.user.info.infra.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지갑 서비스-Repository 통합테스트")
@ActiveProfiles({"dev", "it", "h2-mem"})
@Transactional
@SpringBootTest
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    private int buyUserId;

    @BeforeEach
    void setUp() {
        // given
        buyUserId = 3;
        testAccount = Account.of(buyUserId, 0.0);
        accountRepository.findById(buyUserId).ifPresent(accountRepository::delete);
        accountRepository.save(testAccount);
        walletRepository.deleteAll(walletRepository.findByAccountId(testAccount.getId()));

        Wallet testWallet = Wallet.of("BTC", testAccount.getId(), 0.0, 1000.0);
        walletRepository.save(testWallet);
    }

    @AfterEach
    void tearDown() {
        accountRepository.findById(buyUserId).ifPresent(accountRepository::delete);
        walletRepository.deleteAll(walletRepository.findByAccountId(testAccount.getId()));
    }

    @DisplayName("계좌 ID로 조회 시 지갑이 정상 반환된다.")
    @Test
    void findByAccountId_thenReturnWallet() {
        // when
        List<Wallet> wallets = walletService.findByAccountId(testAccount.getId());

        // then
        assertThat(wallets).isNotEmpty();
        assertThat(wallets.getFirst().getTicker()).isEqualTo("BTC");
    }

    @DisplayName("지갑이 성공적으로 저장된다.")
    @Test
    void save_thenCreateNewWallet() {
        // when
        Wallet newWallet = Wallet.of("TRUMP", testAccount.getId(), 0.0, 5000.0);
        Wallet savedWallet = walletService.save(newWallet);

        // then
        assertThat(savedWallet.getId()).isNotNull();
        assertThat(savedWallet.getTicker()).isEqualTo("TRUMP");
        assertThat(savedWallet.getSize()).isEqualTo(5000.0);
    }

    @DisplayName("유저 ID, 티커로 존재하는 지갑 조회 시 정상적으로 반환된다.")
    @Test
    void findWalletByUserIdAndTicker_ExistingWallet_thenReturnWallet() {
        // when
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");

        // then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getId()).isNotNull();
        assertThat(wallet.getTicker()).isEqualTo("BTC");
        assertThat(wallet.getSize()).isEqualTo(1000.0);
    }

    @DisplayName("유저 ID, 티커로 존재하지 않는 지갑 조회 시 빈 지갑이 새로 반환된다.")
    @Test
    void findWalletByUserIdAndTicker_NonExistingWallet_thenReturnEmptyWallet() {
        // when
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "TRUMP");

        // then
        assertThat(wallet).isNotNull();
        assertThat(wallet.getTicker()).isEqualTo("TRUMP");
        assertThat(wallet.getSize()).isEqualTo(0.0);
    }

    @DisplayName("빈 지갑에 신규 거래가 발생되어 지갑이 정상 업데이트된다.")
    @Test
    void updateWalletAfterTrade_EmptyWalletNewTrade_thenReturnWallet() {
        // given
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");
        wallet.reset(0);
        double tradedPrice = 160_000_000.0;
        double tradedSize = 100.0;

        // when
        walletService.updateWalletAfterTrade(buyUserId, "BTC", tradedPrice, tradedSize);

        // then
        Wallet walletAfter = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");

        assertThat(walletAfter.getBuyPrice()).isEqualTo(tradedPrice);
        assertThat(walletAfter.getSize()).isEqualTo(tradedSize);
    }

    @DisplayName("지갑에 신규 거래(동일 가격, 동일 수량)가 발생되어 지갑이 정상 업데이트된다.")
    @Test
    void updateWalletAfterTrade_SamePriceSameSizeNewTrade_thenReturnWallet() {
        // given
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");
        wallet.reset(0);
        double initialPrice = 160_000_000.0;
        double initialSize = 100.0;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", initialPrice, initialSize);

        // when
        double tradedPrice = initialPrice;
        double tradedSize = initialSize;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", tradedPrice, tradedSize);

        // then
        Wallet walletAfter = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");

        double priceAfter = (initialPrice * initialSize + tradedPrice * tradedSize) / (initialSize + tradedSize);
        assertThat(walletAfter.getBuyPrice()).isEqualTo(priceAfter);
        assertThat(walletAfter.getSize()).isEqualTo(initialSize + tradedSize);
    }

    @DisplayName("지갑에 신규 거래(동일 가격)가 발생되어 지갑이 정상 업데이트된다.")
    @Test
    void updateWalletAfterTrade_SamePriceNewTrade_thenReturnWallet() {
        // given
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");
        wallet.reset(0);
        double initialPrice = 160_000_000.0;
        double initialSize = 100.0;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", initialPrice, initialSize);

        // when
        double tradedPrice = initialPrice;
        double tradedSize = 50.0;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", tradedPrice, tradedSize);

        // then
        Wallet walletAfter = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");

        double priceAfter = (initialPrice * initialSize + tradedPrice * tradedSize) / (initialSize + tradedSize);
        assertThat(walletAfter.getBuyPrice()).isEqualTo(priceAfter);
        assertThat(walletAfter.getSize()).isEqualTo(initialSize + tradedSize);
    }

    @DisplayName("지갑에 신규 거래(동일 수량)가 발생되어 지갑이 정상 업데이트된다.")
    @Test
    void updateWalletAfterTrade_SameSizeNewTrade_thenReturnWallet() {
        // given
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");
        wallet.reset(0);
        double initialPrice = 150_000_000.0;
        double initialSize = 100.0;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", initialPrice, initialSize);

        // when
        double tradedPrice = 160_000_000.0;
        double tradedSize = initialSize;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", tradedPrice, tradedSize);

        // then
        Wallet walletAfter = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");

        double priceAfter = (initialPrice * initialSize + tradedPrice * tradedSize) / (initialSize + tradedSize);
        assertThat(walletAfter.getBuyPrice()).isEqualTo(priceAfter);
        assertThat(walletAfter.getSize()).isEqualTo(initialSize + tradedSize);
    }

    @DisplayName("지갑에 신규 거래가 발생되어 지갑이 정상 업데이트된다.")
    @Test
    void updateWalletAfterTrade_NewTrade_thenReturnWallet() {
        // given
        Wallet wallet = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");
        wallet.reset(0);
        double initialPrice = 150_000_000.0;
        double initialSize = 100.0;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", initialPrice, initialSize);

        // when
        double tradedPrice = 160_000_000.0;
        double tradedSize = 50.0;
        walletService.updateWalletAfterTrade(buyUserId, "BTC", tradedPrice, tradedSize);

        // then
        Wallet walletAfter = walletService.findWalletByUserIdAndTicker(buyUserId, "BTC");

        double priceAfter = (initialPrice * initialSize + tradedPrice * tradedSize) / (initialSize + tradedSize);
        assertThat(walletAfter.getBuyPrice()).isEqualTo(priceAfter);
        assertThat(walletAfter.getSize()).isEqualTo(initialSize + tradedSize);
    }

}
