package com.cleanengine.coin.mypage.controller;

import com.cleanengine.coin.common.response.ApiResponse;
import com.cleanengine.coin.mypage.dto.PagedRankingsDto;
import com.cleanengine.coin.mypage.dto.RankingDto;
import com.cleanengine.coin.mypage.service.RankingSchedulerService;
import com.cleanengine.coin.user.login.infra.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking")
public class RankingController {
    private final RankingSchedulerService rankingSchedulerService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RankingDto>>> getRanking(@AuthenticationPrincipal CustomOAuth2User user) {
        Integer userId = user.getUserId();
        List<RankingDto> rankingDtos = rankingSchedulerService.getMyRanking(userId);
        return ApiResponse.success(rankingDtos, HttpStatus.OK).toResponseEntity();
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PagedRankingsDto>> getAllRanking(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "10") int size) {
        PagedRankingsDto rankingDtos = rankingSchedulerService.getAllRanking(page,size);
        return ApiResponse.success(rankingDtos, HttpStatus.OK).toResponseEntity();
    }

}
