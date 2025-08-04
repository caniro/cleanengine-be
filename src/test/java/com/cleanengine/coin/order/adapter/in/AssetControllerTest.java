package com.cleanengine.coin.order.adapter.in;

import com.cleanengine.coin.base.ControllerTest;
import com.cleanengine.coin.order.application.dto.AssetInfo;
import com.cleanengine.coin.order.application.AssetService;
import com.cleanengine.coin.order.adapter.in.web.asset.AssetController;
import com.cleanengine.coin.tool.annotation.WithCustomMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
public class AssetControllerTest extends ControllerTest {

    @MockitoBean
    AssetService assetService;

    @Test
    @WithCustomMockUser
    public void findAll() throws Exception {
        when(assetService.getAllAssetInfos())
                .thenReturn(List.of(new AssetInfo("BTC", "비트코인", null, null, null)));

        String responseStr = performGet("/api/asset")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AssetController.AssetInfos assetInfos = convertAs(responseStr, AssetController.AssetInfos.class);

        assertEquals(1, assetInfos.assets().size());
        assertEquals("비트코인", assetInfos.assets().get(0).name());
    }

    @Test
    @WithCustomMockUser
    public void findAsset() throws Exception {
        when(assetService.getAssetInfo("BTC"))
                .thenReturn(new AssetInfo("BTC", "비트코인", null, null, null));

        String responseStr = performGet("/api/asset/BTC")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AssetInfo assetInfo = convertAs(responseStr, AssetInfo.class);
        assertEquals("비트코인", assetInfo.name());
    }
}
