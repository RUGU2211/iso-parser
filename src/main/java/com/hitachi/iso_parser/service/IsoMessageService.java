package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.dto.IsoParseResult;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.entity.IsoAudit;
import com.hitachi.iso_parser.parser.Iso8583MessageParser;
import com.hitachi.iso_parser.parser.XmlLimitParser;
import com.hitachi.iso_parser.repository.CardLimitRepository;
import com.hitachi.iso_parser.repository.IsoAuditRepository;
import com.hitachi.iso_parser.util.IsoFieldFormatter;

@Service
public class IsoMessageService {

    private Iso8583MessageParser isoParser;
    private XmlLimitParser xmlParser;
    private LimitCalculationService limitCalculationService;
    private CardLimitRepository cardLimitRepository;
    private IsoAuditRepository isoAuditRepository;
    private IsoConfig isoConfig;

    public IsoMessageService(Iso8583MessageParser isoParser, XmlLimitParser xmlParser,
            LimitCalculationService limitCalculationService, CardLimitRepository cardLimitRepository,
            IsoAuditRepository isoAuditRepository, IsoConfig isoConfig) {
        this.isoParser = isoParser;
        this.xmlParser = xmlParser;
        this.limitCalculationService = limitCalculationService;
        this.cardLimitRepository = cardLimitRepository;
        this.isoAuditRepository = isoAuditRepository;
        this.isoConfig = isoConfig;
    }

    @Transactional
    public IsoParseResponse processIsoMessage(String hexMessage) {
        IsoAudit audit = new IsoAudit();
        audit.setReqIn(hexMessage);
        audit.setBinaryHex(hexMessage);
        audit.setCreatedAt(LocalDateTime.now());

        try {
            IsoParseResult parseResult = isoParser.parse(hexMessage);
            audit.setDe11(parseResult.getDe11());
            if (parseResult.getIsoMsg() != null) {
                audit.setIsoFieldsFormatted(IsoFieldFormatter.format(parseResult.getIsoMsg()));
            }

            String xml = parseResult.getXml();
            CardLimitDTO dto = xmlParser.parse(xml);

            audit.setPan(dto.getPan());
            audit.setExpiryDate(dto.getExpiryDate());
            audit.setSeqNr(dto.getSeqNr());
            audit.setCashLimit(dto.getCashLimit());
            audit.setGoodsLimit(dto.getGoodsLimit());
            audit.setCardNotPresentLimit(dto.getCardNotPresentLimit());

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

            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(true);
            response.setMessage("ISO message processed successfully");
            response.setDe39(isoConfig.getDe39Success());

            audit.setRespOut(toJsonResponse(response));
            audit.setDe39(isoConfig.getDe39Success());
            isoAuditRepository.save(audit);

            return response;
        } catch (Exception e) {
            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setDe39(isoConfig.getDe39Failed());

            audit.setRespOut(toJsonResponse(response));
            audit.setDe39(isoConfig.getDe39Failed());
            isoAuditRepository.save(audit);

            return response;
        }
    }

    private String toJsonResponse(IsoParseResponse r) {
        return "{\"success\":" + r.isSuccess() + ",\"message\":\"" + escape(r.getMessage()) + "\",\"de39\":\"" + (r.getDe39() != null ? r.getDe39() : "") + "\"}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
