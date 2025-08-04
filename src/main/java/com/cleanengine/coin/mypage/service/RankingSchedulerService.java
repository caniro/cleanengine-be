package com.cleanengine.coin.mypage.service;

import com.cleanengine.coin.mypage.dto.PagedRankingsDto;
import com.cleanengine.coin.mypage.dto.RankingDto;
import com.cleanengine.coin.mypage.infra.CurrentPriceCache;
import com.cleanengine.coin.mypage.repository.MyRankingRepository;
import com.cleanengine.coin.user.domain.Account;
import com.cleanengine.coin.user.domain.Wallet;
import com.cleanengine.coin.user.info.infra.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.cleanengine.coin.common.CommonValues.BUY_ORDER_BOT_ID;
import static com.cleanengine.coin.common.CommonValues.SELL_ORDER_BOT_ID;

@RequiredArgsConstructor
@Service
public class RankingSchedulerService {

    private final MyRankingRepository myRankingRepository;
    private final AccountRepository accountRepository;
    private final Map<Integer,Double> roiCache = new ConcurrentHashMap<>();
    private final CurrentPriceCache currentPriceCache;
    private List<RankingDto> rankingDtoList = new ArrayList<>();
    private static final Set<Integer> EXCLUDED_USER_IDS = Set.of(BUY_ORDER_BOT_ID,SELL_ORDER_BOT_ID);



    @Scheduled(fixedRate = 1000)
    public void getROIRanking(){
        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            Integer userId = account.getUserId();
            double cash = account.getCash();

            List<Wallet> wallets = myRankingRepository.findAllByAccountId(userId);
            double marketValue = 0;
            double purchaseValue = 0;
            for(Wallet wallet : wallets){
                String ticker = wallet.getTicker();
                double size = Optional.ofNullable(wallet.getSize()).orElse(0.0);
                double buyPrice = Optional.ofNullable(wallet.getBuyPrice()).orElse(0.0);
                double currentPrice = currentPriceCache.getCurrentPrice(ticker);

                marketValue += size*currentPrice;
                purchaseValue += size*buyPrice;
            }
            double totalValue = cash + purchaseValue;
            double roi = (purchaseValue == 0) ? 0 : ((totalValue - purchaseValue / purchaseValue) * 100);
            roiCache.put(userId, roi);
        }
        getRankingAll();
    }



    public void getRankingAll(){
        AtomicInteger rank = new AtomicInteger(1);

        rankingDtoList = roiCache.entrySet().stream()
                .filter(entry -> !EXCLUDED_USER_IDS.contains(entry.getKey())) //봇 유저 필터
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()) //value값으로 랭킹 정렬
                .map(e -> new RankingDto(
                        rank.getAndIncrement(),
                        e.getKey(),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }
    public PagedRankingsDto getAllRanking(int page, int size){
        List<RankingDto> currentRankingList = this.rankingDtoList;
        long totalElements = currentRankingList.size();
        int totalPages = (int)Math.ceil((double)totalElements / size);

        //유효성
        int adjustedPage = page -1;

        if (adjustedPage<0) adjustedPage = 0;
        if (size<=0) size = 10;
        if (adjustedPage >= totalPages && totalPages > 0) {
            adjustedPage = totalPages - 1; //마지막 페이지 넘어가기 방어
            }


        //유저가 없으면 빈 배열
        if (totalElements == 0) { // +page 요청 수 넘어가면 추가 방어
            return new PagedRankingsDto(totalPages,totalElements,page,size,Collections.emptyList());
        }

        int startIndex = adjustedPage * size;
        int endIndex = Math.min(startIndex + size, (int) totalElements);

        List<RankingDto> pageContent;
        if (startIndex >= totalElements) {
            pageContent = Collections.emptyList();
        } else {
            pageContent = currentRankingList.subList(startIndex, endIndex);
        }

        return new PagedRankingsDto(totalPages,totalElements,page,size,pageContent);
    }
    public List<RankingDto> getMyRanking(Integer userId){
        int myIndex = IntStream.range(0, rankingDtoList.size())
                .filter(i -> rankingDtoList.get(i).getId().equals(userId))
                .findFirst()
                .orElse(-1);

        if (myIndex == -1) {
            return Collections.emptyList(); // 유저가 랭킹에 없음
        }

        int from = Math.max(0, myIndex - 2);
        int to = Math.min(rankingDtoList.size(), myIndex + 3); // +3은 본인 포함하여 아래 2명까지

        return rankingDtoList.subList(from, to);
    }
}
