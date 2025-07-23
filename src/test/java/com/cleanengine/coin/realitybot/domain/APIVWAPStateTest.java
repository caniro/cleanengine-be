package com.cleanengine.coin.realitybot.domain;

import com.cleanengine.coin.realitybot.dto.Ticks;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class APIVWAPStateTest {

    @Mock
    VWAPCalculator calculator;
    @InjectMocks
    APIVWAPState apivwapState;

    @DisplayName("ticks가 10개 이하 일 경우 ticks 갯수만큼 record 작동")
    @Test
    void testAddTicksUnder10() {
        //given
        for (int i = 0; i < 5; i++) {
            apivwapState.addTick(new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",100,i,180.0f,5.5,"ASK", 100003L));
        }
        //when

        //then
        assertEquals(5,apivwapState.getTickSize());
    }

    @DisplayName("ticks가 10개 초과 시 오래된 tick 제거 후 size 유지")
    @Test
    void testAddTicksOver10(){
        APIVWAPState apivwapState = new APIVWAPState();//mock이 아니라 진짜 객체 생성
        //given
        Ticks firstTick = new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",1000,10,180.0f,5.5,"ASK", 100003L);
        apivwapState.addTick(firstTick);
        for (int i = 0; i < 10; i++) {
            apivwapState.addTick(new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",100,i,180.0f,5.5,"ASK", 100003L));
        }
        assertEquals(100,apivwapState.getVWAP());

       Ticks lastTick = new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",999,9,180.0f,5.5,"ASK", 100003L);
        apivwapState.addTick(lastTick);
        //when

        //then
        assertEquals(10,apivwapState.getTickSize());
        assertEquals(249.83,apivwapState.getVWAP(),0.1);
    }


    @DisplayName("평균 주문 갯수로 계산한다.")
    @Test
    @Disabled
    void testGetAvgVolume(){
        APIVWAPState apivwapState = new APIVWAPState();//mock이 아니라 진짜 객체 생성
        //given
        Ticks firstTick = new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",1000,10,180.0f,5.5,"ASK", 100003L);
        apivwapState.addTick(firstTick);
        for (int i = 0; i < 10; i++) {
            apivwapState.addTick(new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",i,10,180.0f,5.5,"ASK", 100003L));
        }
        Ticks lastTick = new Ticks("BTC","2025-06-01","11:32:45","2025-06-01T11:32:45.789Z",999,10,180.0f,5.5,"ASK", 100003L);
        apivwapState.addTick(lastTick);
        //when

        //then
        assertEquals(2,apivwapState.getAvgVolumePerOrder());
    }
}
