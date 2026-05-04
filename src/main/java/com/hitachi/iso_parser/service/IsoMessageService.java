package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.hitachi.iso_parser.util.ExtraLimitsJsonMapper;
import com.hitachi.iso_parser.util.IsoFieldFormatter;

@Service
public class IsoMessageService {

    private final Iso8583MessageParser isoParser;
    private final XmlLimitParser xmlParser;
    private final LimitCalculationService limitCalculationService;
    private final CardLimitRepository cardLimitRepository;
    private final IsoAuditRepository isoAuditRepository;
    private final IsoConfig isoConfig;
    private final ExtraLimitsJsonMapper extraLimitsJsonMapper;
    private final ObjectMapper auditObjectMapper = new ObjectMapper();

    public IsoMessageService(Iso8583MessageParser isoParser, XmlLimitParser xmlParser,
            LimitCalculationService limitCalculationService, CardLimitRepository cardLimitRepository,
            IsoAuditRepository isoAuditRepository, IsoConfig isoConfig,
            ExtraLimitsJsonMapper extraLimitsJsonMapper) {
        this.isoParser = isoParser;
        this.xmlParser = xmlParser;
        this.limitCalculationService = limitCalculationService;
        this.cardLimitRepository = cardLimitRepository;
        this.isoAuditRepository = isoAuditRepository;
        this.isoConfig = isoConfig;
        this.extraLimitsJsonMapper = extraLimitsJsonMapper;
    }

    @Transactional
    public IsoParseResponse processIsoMessage(String hexMessage) {
        return processIsoMessage(hexMessage, null);
    }

    @Transactional
    public IsoParseResponse processIsoMessage(String hexMessage, String requestUser) {
        long startNs = System.nanoTime();
        IsoAudit audit = new IsoAudit();
        audit.setReqIn(hexMessage);
        audit.setBinaryHex(hexMessage);
        audit.setCreatedAt(LocalDateTime.now());
        audit.setRequestId(UUID.randomUUID().toString());
        String actor = trimUser(isoConfig.getLastUpdUser());

        CardLimitDTO dto = null;
        LimitEngineResult limitResult = null;
        try {
            dto = parseInputToCardLimitDto(hexMessage, audit);

            audit.setPan(dto.getPan());
            audit.setExpiryDate(dto.getExpiryDate());
            audit.setSeqNr(dto.getSeqNr());
            audit.setCashLimit(dto.getLimitValue("cash_limit"));
            audit.setGoodsLimit(dto.getLimitValue("goods_limit"));
            audit.setCardNotPresentLimit(dto.getLimitValue("card_not_present_limit"));

            limitResult = limitCalculationService.buildLimitResult(dto);
            String limitString = limitResult.getLimitPayload();

            String pan = dto.getPan();
            String seqNr = (dto.getSeqNr() != null && !dto.getSeqNr().isEmpty()) ? dto.getSeqNr() : isoConfig.getDefaultSeqNr();
            Integer issuerNr = isoConfig.getDefaultNr();

            CardLimit entity = cardLimitRepository
                    .findByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(issuerNr, pan, seqNr)
                    .orElse(new CardLimit());

            LocalDateTime now = LocalDateTime.now();
            if (entity.getId() == null) {
                entity.setCreatedDate(now);
            }
            entity.setIssuerNr(issuerNr);
            entity.setPan(pan);
            entity.setSeqNr(seqNr);
            entity.setLimits(limitString);
            entity.setLimitExtraData(buildExtraJson(limitResult));
            entity.setLastUpdatedDate(now);
            entity.setLastUpdatedUser(actor);
            entity.setDateDeleted(null);

            cardLimitRepository.save(entity);

            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(true);
            response.setMessage("ISO message processed successfully");
            response.setDe39(isoConfig.getDe39Success());

            audit.setRespOut(toJsonResponse(response));
            audit.setDe39(isoConfig.getDe39Success());
            audit.setProcessingTimeMs((System.nanoTime() - startNs) / 1_000_000);
            audit.setApiOperation("ISO_PARSE");
            audit.setApiDetail(buildIsoParseApiDetail(true, actor, dto, limitResult, null));
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
            audit.setApiOperation("ISO_PARSE");
            audit.setApiDetail(buildIsoParseApiDetail(false, actor, dto, limitResult, e.getMessage()));
            isoAuditRepository.save(audit);

            return response;
        }
    }

    private String buildIsoParseApiDetail(boolean success, String requestUser, CardLimitDTO dto, LimitEngineResult limitResult, String error) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("resource", "ISO");
        m.put("success", success);
        m.put("requestedByUser", requestUser);
        if (dto != null) {
            m.put("pan", dto.getPan());
            m.put("seqNr", dto.getSeqNr());
        }
        if (limitResult != null) {
            m.put("knownSegmentCount", limitResult.getKnownSegments().size());
            m.put("unknownSegmentCount", limitResult.getUnknownSegments().size());
            String payload = limitResult.getLimitPayload();
            m.put("limitPayloadLen", payload != null ? payload.length() : 0);
        }
        if (error != null) {
            m.put("error", error.length() > 2000 ? error.substring(0, 2000) : error);
        }
        try {
            String json = auditObjectMapper.writeValueAsString(m);
            return json.length() > 15500 ? json.substring(0, 15500) + "…" : json;
        } catch (JsonProcessingException ex) {
            return "{\"resource\":\"ISO\",\"detailSerializeError\":true}";
        }
    }

    private String buildExtraJson(LimitEngineResult limitResult) {
        Map<String, String> extra = new LinkedHashMap<>();
        for (ResolvedLimitSegment segment : limitResult.getUnknownSegments()) {
            extra.put(segment.getFieldName(), segment.getValue());
        }
        return extraLimitsJsonMapper.toJson(extra);
    }

    private String trimUser(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 20) {
            return value;
        }
        return value.substring(0, 20);
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

    private String toJsonResponse(IsoParseResponse r) {
        return "{\"success\":" + r.isSuccess() + ",\"message\":\"" + escape(r.getMessage()) + "\",\"de39\":\""
                + (r.getDe39() != null ? r.getDe39() : "") + "\"}";
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
