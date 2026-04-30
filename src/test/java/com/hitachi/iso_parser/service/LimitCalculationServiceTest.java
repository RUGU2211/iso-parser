package com.hitachi.iso_parser.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.hitachi.iso_parser.config.LimitConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.entity.LimitMaster;
import com.hitachi.iso_parser.repository.LimitMasterRepository;

class LimitCalculationServiceTest {

    private LimitMasterRepository limitMasterRepository;
    private LimitCalculationService service;

    @BeforeEach
    void setUp() {
        limitMasterRepository = Mockito.mock(LimitMasterRepository.class);
        LimitConfig config = new LimitConfig();
        config.setMax12("999999999999");
        config.setDefaultValue("0");
        config.setDefaultProfileNr(42);
        config.setDefaultRuleNr(52);
        service = new LimitCalculationService(limitMasterRepository, config);
    }

    @Test
    void buildLimitResult_shouldSortByPriorityAndSplitUnknownFields() {
        LimitMaster goods = new LimitMaster();
        goods.setLimitName("goods_limit");
        goods.setLimitPnr(42);
        goods.setLimitRuleNr(52);
        goods.setPriority(20);

        LimitMaster cash = new LimitMaster();
        cash.setLimitName("cash_limit");
        cash.setLimitPnr(42);
        cash.setLimitRuleNr(54);
        cash.setPriority(10);

        when(limitMasterRepository.findByIsActiveTrueOrderByPriorityAsc()).thenReturn(List.of(cash, goods));

        CardLimitDTO dto = new CardLimitDTO();
        dto.putLimit("goods_limit", "80000");
        dto.putLimit("cash_limit", "20000");
        dto.putLimit("upi_limit", "5000");

        LimitEngineResult result = service.buildLimitResult(dto);

        assertEquals(2, result.getKnownSegments().size());
        assertEquals("cash_limit", result.getKnownSegments().get(0).getFieldName());
        assertEquals("goods_limit", result.getKnownSegments().get(1).getFieldName());
        assertEquals(1, result.getUnknownSegments().size());
        assertEquals("upi_limit", result.getUnknownSegments().get(0).getFieldName());
        assertTrue(result.getLimitPayload().contains("20000"));
        assertTrue(result.getLimitPayload().contains("80000"));
        assertFalse(result.getLimitPayload().contains("5000"));
    }
}
