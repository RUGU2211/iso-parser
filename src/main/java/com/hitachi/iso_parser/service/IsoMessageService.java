package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.entity.IsoAudit;
import com.hitachi.iso_parser.parser.Iso8583MessageParser;
import com.hitachi.iso_parser.parser.XmlLimitParser;
import com.hitachi.iso_parser.repository.CardLimitRepository;
import com.hitachi.iso_parser.repository.IsoAuditRepository;
import com.hitachi.iso_parser.util.ExtraLimitsJsonMapper;

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
        IsoAudit audit = new IsoAudit();
        audit.setReqIn(toJsonString(Map.of("request", hexMessage)));
        audit.setCreatedAt(LocalDateTime.now());
        String actor = trimUser(isoConfig.getLastUpdUser());

        CardLimitDTO dto = null;
        try {
            dto = parseInputToCardLimitDto(hexMessage, audit);

            limitCalculationService.buildLimitResult(dto);

            String pan = dto.getPan();
            String seqNr = (dto.getSeqNr() != null && !dto.getSeqNr().isEmpty()) ? dto.getSeqNr() : isoConfig.getDefaultSeqNr();
            Integer issuerNr = isoConfig.getDefaultNr();

            CardLimit entity = cardLimitRepository
                    .findByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(issuerNr, pan, seqNr)
                    .orElse(new CardLimit());
            Map<String, String> mergedData = mergeTotalData(entity.getTotalDataReceived(), dto.getLimits());
            String limitString = buildKnownPayloadFromData(mergedData);

            LocalDateTime now = LocalDateTime.now();
            if (entity.getId() == null) {
                entity.setCreatedDate(now);
            }
            entity.setIssuerNr(issuerNr);
            entity.setPan(pan);
            entity.setSeqNr(seqNr);
            entity.setLimits(limitString);
            entity.setTotalDataReceived(extraLimitsJsonMapper.toJson(mergedData));
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

        return xmlParser.parse(isoParser.parse(normalized).getXml());
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
        Map<String, String> out = new LinkedHashMap<>();
        out.put("message", r.getMessage());
        out.put("de39", r.getDe39());
        out.put("status", r.isSuccess() ? "success" : "failed");
        return toJsonString(out);
    }

    private String toJsonString(Map<?, ?> payload) {
        try {
            return auditObjectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, String> mergeTotalData(String existingJson, Map<String, String> incomingLimits) {
        Map<String, String> merged = new LinkedHashMap<>(extraLimitsJsonMapper.fromJson(existingJson));
        if (incomingLimits != null) {
            incomingLimits.forEach((k, v) -> {
                if (k != null && !k.isBlank()) {
                    merged.put(k, v != null ? v.trim() : "");
                }
            });
        }
        return merged;
    }

    private String buildKnownPayloadFromData(Map<String, String> allData) {
        if (allData == null || allData.isEmpty()) {
            return "";
        }
        CardLimitDTO mergedDto = new CardLimitDTO();
        mergedDto.setLimits(allData);
        return limitCalculationService.buildLimitResult(mergedDto).getLimitPayload();
    }
}
