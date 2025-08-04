package com.cleanengine.coin.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PagedRankingsDto {
    private int totalPages;
    private Long totalElements;
    private int currentPage;
    private int pageSize;
    private List<RankingDto> rankings;
}
