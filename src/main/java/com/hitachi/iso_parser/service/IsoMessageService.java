package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.parser.Iso8583MessageParser;
import com.hitachi.iso_parser.parser.XmlLimitParser;
import com.hitachi.iso_parser.repository.CardLimitRepository;

@Service
public class IsoMessageService {

    private Iso8583MessageParser isoParser;
    private XmlLimitParser xmlParser;
    private LimitCalculationService limitCalculationService;
    private CardLimitRepository cardLimitRepository;
    private IsoConfig isoConfig;

    public IsoMessageService(Iso8583MessageParser isoParser, XmlLimitParser xmlParser,
            LimitCalculationService limitCalculationService, CardLimitRepository cardLimitRepository,
            IsoConfig isoConfig) {
        this.isoParser = isoParser;
        this.xmlParser = xmlParser;
        this.limitCalculationService = limitCalculationService;
        this.cardLimitRepository = cardLimitRepository;
        this.isoConfig = isoConfig;
    }

    @Transactional
    public void processIsoMessage(String hexMessage) {
        String xml = isoParser.extractXml(hexMessage);
        CardLimitDTO dto = xmlParser.parse(xml);
        String limitString = limitCalculationService.buildLimitString(dto);

        String pan = dto.getPan();
        String seqNr = (dto.getSeqNr() != null && !dto.getSeqNr().isEmpty()) ? dto.getSeqNr() : isoConfig.getDefaultSeqNr();

        CardLimit entity = cardLimitRepository.findByPanAndSeqNr(pan, seqNr).orElse(new CardLimit());

        entity.setIsoNr(isoConfig.getDefaultNr());
        entity.setPan(pan);
        entity.setSeqNr(seqNr);
        entity.setLimits(limitString);
        entity.setLastUpdDate(LocalDateTime.now());
        entity.setLastUpdUser(isoConfig.getLastUpdUser());

        cardLimitRepository.save(entity);
    }
}
