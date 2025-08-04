package com.cleanengine.coin.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PagedOrderListDto {
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;
    private List<OrderListDto> orderList;
}
