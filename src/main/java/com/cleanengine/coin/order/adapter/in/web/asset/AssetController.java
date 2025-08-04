package com.cleanengine.coin.order.adapter.in.web.asset;

import com.cleanengine.coin.common.response.ApiResponse;
import com.cleanengine.coin.order.application.dto.AssetInfo;
import com.cleanengine.coin.order.application.AssetService;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;

    @GetMapping("/api/asset/{ticker}")
    public ResponseEntity<ApiResponse<AssetInfo>> getAsset(@PathVariable String ticker) {
        return ApiResponse.success(assetService.getAssetInfo(ticker), HttpStatus.OK).toResponseEntity();
    }

    @Operation(summary = "모든 종목 정보를 불러옵니다.")
    @GetMapping("/api/asset")
    public ResponseEntity<ApiResponse<AssetInfos>> getAllAsset() {
        List<AssetInfo> assetInfoList = assetService.getAllAssetInfos();
        return ApiResponse.success(AssetInfos.of(assetInfoList), HttpStatus.OK).toResponseEntity();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssetInfos(
            List<AssetInfo> assets
    ){
        public static AssetInfos of(List<AssetInfo> assets) {
            return new AssetInfos(assets);
        }
    }
}
