package com.cleanengine.coin.user.info.application;

import com.cleanengine.coin.order.application.AssetService;
import com.cleanengine.coin.user.domain.Account;
import com.cleanengine.coin.user.domain.OAuth;
import com.cleanengine.coin.user.domain.Wallet;
import com.cleanengine.coin.user.info.infra.AccountRepository;
import com.cleanengine.coin.user.info.infra.OAuthRepository;
import com.cleanengine.coin.user.info.infra.WalletRepository;
import com.cleanengine.coin.user.info.presentation.UserInfoDTO;
import com.cleanengine.coin.user.info.presentation.UserWalletDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService {

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final OAuthRepository oAuthRepository;
    private final AssetService assetService;

    @Transactional(readOnly = true)
    public UserInfoDTO retrieveUserInfoByUserId(Integer userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다. userId: " + userId));
        OAuth oauth = oAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("OAuth 정보를 찾을 수 없습니다. userId: " + userId));

        // TODO : 모든 종목에 대해 없는 지갑은 생성... 근데 어디서?
        List<Wallet> wallets = walletRepository.findByAccountId(account.getId());

        // 총 자산 계산 (현금 + (각 코인 보유량 * 현재가))
        double totalWalletValue = wallets.stream()
                .mapToDouble(wallet ->
                {
                    Double currentPrice = assetService.getCurrentPrice(wallet.getTicker());
                    return currentPrice == null ? 0.0 : wallet.getSize() * currentPrice;
                })
                .sum();
        double totalCash = account.getCash() + totalWalletValue;

        List<UserWalletDTO> userWalletDTOs = convertToDTO(wallets);
        return UserInfoDTO.of(userId, oauth.getEmail(), oauth.getNickname(), oauth.getProvider(), account.getCash(), userWalletDTOs, totalCash);
    }

    private List<UserWalletDTO> convertToDTO(List<Wallet> wallets) {
        return wallets.stream()
                .filter(w -> w.getSize() > 0.0)
                .map(w -> {
                    Double currentPrice = assetService.getCurrentPrice(w.getTicker());
                    Double roi;
                    if (currentPrice == null || w.getBuyPrice() == 0.0) {
                        roi = null;
                    } else {
                        roi = (currentPrice / w.getBuyPrice() - 1) * 100;
                    }

                    return UserWalletDTO.of(w.getTicker(),
                            assetService.getAssetName(w.getTicker()),
                            w.getAccountId(),
                            w.getSize(),
                            w.getBuyPrice(),
                            roi,
                            currentPrice);
                })
                .toList();
    }

}
