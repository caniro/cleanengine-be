package com.cleanengine.coin.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingDto {
    private Integer ranking;
    private Integer id;
    private Double roi;
}
