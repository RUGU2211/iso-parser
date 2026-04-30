package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.dto.IsoParseResult;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.entity.IsoAudit;
import com.hitachi.iso_parser.entity.LimitExtraData;
import com.hitachi.iso_parser.parser.Iso8583MessageParser;
import com.hitachi.iso_parser.parser.XmlLimitParser;
import com.hitachi.iso_parser.repository.CardLimitRepository;
import com.hitachi.iso_parser.repository.IsoAuditRepository;
import com.hitachi.iso_parser.repository.LimitExtraDataRepository;
import com.hitachi.iso_parser.util.IsoFieldFormatter;

@Service
public class IsoMessageService {

    private Iso8583MessageParser isoParser;
    private XmlLimitParser xmlParser;
    private LimitCalculationService limitCalculationService;
    private CardLimitRepository cardLimitRepository;
    private LimitExtraDataRepository limitExtraDataRepository;
    private IsoAuditRepository isoAuditRepository;
    private IsoConfig isoConfig;

    public IsoMessageService(Iso8583MessageParser isoParser, XmlLimitParser xmlParser,
            LimitCalculationService limitCalculationService, CardLimitRepository cardLimitRepository,
            LimitExtraDataRepository limitExtraDataRepository,
            IsoAuditRepository isoAuditRepository, IsoConfig isoConfig) {
        this.isoParser = isoParser;
        this.xmlParser = xmlParser;
        this.limitCalculationService = limitCalculationService;
        this.cardLimitRepository = cardLimitRepository;
        this.limitExtraDataRepository = limitExtraDataRepository;
        this.isoAuditRepository = isoAuditRepository;
        this.isoConfig = isoConfig;
    }

    @Transactional
    public IsoParseResponse processIsoMessage(String hexMessage) {
        long startNs = System.nanoTime();
        IsoAudit audit = new IsoAudit();
        audit.setReqIn(hexMessage);
        audit.setBinaryHex(hexMessage);
        audit.setCreatedAt(LocalDateTime.now());
        audit.setRequestId(UUID.randomUUID().toString());

        try {
            CardLimitDTO dto = parseInputToCardLimitDto(hexMessage, audit);

            audit.setPan(dto.getPan());
            audit.setExpiryDate(dto.getExpiryDate());
            audit.setSeqNr(dto.getSeqNr());
            audit.setCashLimit(dto.getLimitValue("cash_limit"));
            audit.setGoodsLimit(dto.getLimitValue("goods_limit"));
            audit.setCardNotPresentLimit(dto.getLimitValue("card_not_present_limit"));

            LimitEngineResult limitResult = limitCalculationService.buildLimitResult(dto);
            String limitString = limitResult.getLimitPayload();

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
            persistStructuredLimits(pan, seqNr, limitResult);

            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(true);
            response.setMessage("ISO message processed successfully");
            response.setDe39(isoConfig.getDe39Success());

            audit.setRespOut(toJsonResponse(response));
            audit.setDe39(isoConfig.getDe39Success());
            audit.setProcessingTimeMs((System.nanoTime() - startNs) / 1_000_000);
            isoAuditRepository.save(audit);

            return response;
        } catch (Exception e) {
            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            response.setDe39(isoConfig.getDe39Failed());

            audit.setRespOut(toJsonResponse(response));
            audit.setDe39(isoConfig.getDe39Failed());
            audit.setErrorMessage(e.getMessage());
            audit.setProcessingTimeMs((System.nanoTime() - startNs) / 1_000_000);
            isoAuditRepository.save(audit);

            return response;
        }
    }

    private CardLimitDTO parseInputToCardLimitDto(String input, IsoAudit audit) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input message cannot be null or empty");
        }

        String normalized = input.trim();
        if (looksLikeXml(normalized)) {
            return xmlParser.parse(normalized);
        }

        if (containsXmlBlock(normalized)) {
            return xmlParser.parse(extractXmlBlock(normalized));
        }

        IsoParseResult parseResult = isoParser.parse(normalized);
        audit.setDe11(parseResult.getDe11());
        if (parseResult.getIsoMsg() != null) {
            audit.setIsoFieldsFormatted(IsoFieldFormatter.format(parseResult.getIsoMsg()));
        }
        return xmlParser.parse(parseResult.getXml());
    }

    private boolean looksLikeXml(String input) {
        return input.startsWith("<") && input.contains("</");
    }

    private boolean containsXmlBlock(String input) {
        return input.contains("<InquiryOrUpdateData")
                || input.contains("<Postilion:InquiryOrUpdateData");
    }

    private String extractXmlBlock(String input) {
        int start = input.indexOf("<InquiryOrUpdateData");
        String closingTag = "</InquiryOrUpdateData>";

        if (start < 0) {
            start = input.indexOf("<Postilion:InquiryOrUpdateData");
            closingTag = "</Postilion:InquiryOrUpdateData>";
        }

        if (start < 0) {
            throw new IllegalArgumentException("Could not locate XML block in ISO text input");
        }

        int end = input.indexOf(closingTag, start);
        if (end < 0) {
            throw new IllegalArgumentException("Could not locate XML closing tag in ISO text input");
        }

        return input.substring(start, end + closingTag.length());
    }

    private void persistStructuredLimits(String pan, String seqNr, LimitEngineResult limitResult) {
        LocalDateTime now = LocalDateTime.now();

        limitExtraDataRepository.deleteByPanAndSeqNr(pan, seqNr);
        for (ResolvedLimitSegment segment : limitResult.getUnknownSegments()) {
            LimitExtraData extra = new LimitExtraData();
            extra.setPan(pan);
            extra.setSeqNr(seqNr);
            extra.setFieldName(segment.getFieldName());
            extra.setFieldValue(segment.getValue());
            extra.setSource("XML");
            extra.setCreatedAt(now);
            limitExtraDataRepository.save(extra);
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
