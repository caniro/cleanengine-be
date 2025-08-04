package com.cleanengine.coin.mypage.controller;

import com.cleanengine.coin.common.response.ApiResponse;
import com.cleanengine.coin.mypage.dto.PagedOrderListDto;
import com.cleanengine.coin.mypage.service.OrderListService;
import com.cleanengine.coin.user.login.infra.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userinfo")
public class OrderListController {
    private final OrderListService orderListService;
    @GetMapping("/trades")
    public ResponseEntity<ApiResponse<PagedOrderListDto>> getOrderList(@AuthenticationPrincipal CustomOAuth2User user,
                                                                            @RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(defaultValue = "false") boolean settled) {
        Integer userId = user.getUserId();
        PagedOrderListDto orderList = orderListService.getOrderList(userId,page,size,settled);
        return ApiResponse.success(orderList, HttpStatus.OK).toResponseEntity();
    }
}
