package com.hitachi.iso_parser.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.dto.IsoParseResult;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.parser.Iso8583MessageParser;
import com.hitachi.iso_parser.parser.XmlLimitParser;
import com.hitachi.iso_parser.repository.CardLimitRepository;
import com.hitachi.iso_parser.repository.IsoAuditRepository;
import com.hitachi.iso_parser.util.ExtraLimitsJsonMapper;

class IsoMessageServiceTest {

    private IsoMessageService isoMessageService;
    private Iso8583MessageParser isoParser;
    private XmlLimitParser xmlParser;
    private LimitCalculationService limitCalculationService;
    private CardLimitRepository cardLimitRepository;
    private IsoAuditRepository isoAuditRepository;

    @BeforeEach
    void setUp() {
        isoParser = Mockito.mock(Iso8583MessageParser.class);
        xmlParser = Mockito.mock(XmlLimitParser.class);
        limitCalculationService = Mockito.mock(LimitCalculationService.class);
        cardLimitRepository = Mockito.mock(CardLimitRepository.class);
        isoAuditRepository = Mockito.mock(IsoAuditRepository.class);

        IsoConfig isoConfig = new IsoConfig();
        isoConfig.setDefaultNr(13);
        isoConfig.setDefaultSeqNr("001");
        isoConfig.setLastUpdUser("sp");
        isoConfig.setDe39Success("00");
        isoConfig.setDe39Failed("01");

        isoMessageService = new IsoMessageService(
                isoParser,
                xmlParser,
                limitCalculationService,
                cardLimitRepository,
                isoAuditRepository,
                isoConfig,
                new ExtraLimitsJsonMapper());
    }

    @Test
    void processIsoMessage_shouldPersistKnownAndUnknownInSingleCardLimitsRow() {
        IsoParseResult parseResult = new IsoParseResult();
        parseResult.setDe11("123456");
        parseResult.setXml("<InquiryOrUpdateData/>");
        parseResult.setIsoMsg(new ISOMsg());
        when(isoParser.parse(any())).thenReturn(parseResult);

        CardLimitDTO dto = new CardLimitDTO();
        dto.setPan("3538210000000026");
        dto.setExpiryDate("3005");
        dto.setSeqNr("001");
        dto.putLimit("goods_limit", "80000");
        dto.putLimit("unknown_limit", "12345");
        when(xmlParser.parse(any())).thenReturn(dto);

        ResolvedLimitSegment known = new ResolvedLimitSegment();
        known.setFieldName("goods_limit");
        known.setValue("80000");
        known.setProfileNr(42);
        known.setRuleNr(52);
        known.setPriority(10);
        known.setKnown(true);
        known.setSegmentPayload("SEG1");

        ResolvedLimitSegment unknown = new ResolvedLimitSegment();
        unknown.setFieldName("unknown_limit");
        unknown.setValue("12345");
        unknown.setKnown(false);

        LimitEngineResult engineResult = new LimitEngineResult();
        engineResult.setLimitPayload("SEG1");
        engineResult.setKnownSegments(java.util.List.of(known));
        engineResult.setUnknownSegments(java.util.List.of(unknown));
        when(limitCalculationService.buildLimitResult(any())).thenReturn(engineResult);
        when(cardLimitRepository.findByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(13, "3538210000000026", "001"))
                .thenReturn(Optional.empty());

        IsoParseResponse response = isoMessageService.processIsoMessage("A1B2");

        assertTrue(response.isSuccess());
        assertEquals("00", response.getDe39());
        ArgumentCaptor<CardLimit> cardCaptor = ArgumentCaptor.forClass(CardLimit.class);
        verify(cardLimitRepository).save(cardCaptor.capture());
        assertEquals("{\"goods_limit\":\"80000\",\"unknown_limit\":\"12345\"}", cardCaptor.getValue().getTotalDataReceived());
        assertEquals("SEG1", cardCaptor.getValue().getLimits());
        assertEquals(Integer.valueOf(13), cardCaptor.getValue().getIssuerNr());

        verify(isoAuditRepository).save(any());
    }
}
