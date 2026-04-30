package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.dto.LimitRecordResponse;
import com.hitachi.iso_parser.dto.LimitUpsertRequest;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.entity.LimitExtraData;
import com.hitachi.iso_parser.repository.CardLimitRepository;
import com.hitachi.iso_parser.repository.LimitExtraDataRepository;

@Service
public class CardLimitCrudService {

    private CardLimitRepository cardLimitRepository;
    private LimitExtraDataRepository limitExtraDataRepository;
    private LimitCalculationService limitCalculationService;
    private IsoConfig isoConfig;

    public CardLimitCrudService(CardLimitRepository cardLimitRepository,
            LimitExtraDataRepository limitExtraDataRepository,
            LimitCalculationService limitCalculationService,
            IsoConfig isoConfig) {
        this.cardLimitRepository = cardLimitRepository;
        this.limitExtraDataRepository = limitExtraDataRepository;
        this.limitCalculationService = limitCalculationService;
        this.isoConfig = isoConfig;
    }

    public List<LimitRecordResponse> getByPan(String pan) {
        List<CardLimit> rows = cardLimitRepository.findAllByPan(pan);
        List<LimitRecordResponse> response = new ArrayList<>();
        for (CardLimit row : rows) {
            response.add(mapToResponse(row));
        }
        return response;
    }

    @Transactional
    public LimitRecordResponse create(String panPath, LimitUpsertRequest request) {
        String pan = normalize(panPath);
        if (pan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pan is required");
        }
        if (cardLimitRepository.existsByPan(pan)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Record already exists for pan. Use PUT for upsert.");
        }
        request.setPan(pan);
        return saveUpsertInternal(request, false);
    }

    @Transactional
    public LimitRecordResponse upsert(String panPath, LimitUpsertRequest request) {
        request.setPan(normalize(panPath));
        return saveUpsertInternal(request, true);
    }

    @Transactional
    public void delete(String pan) {
        String normalizedPan = normalize(pan);
        if (!cardLimitRepository.existsByPan(normalizedPan)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found for pan");
        }
        limitExtraDataRepository.deleteByPan(normalizedPan);
        cardLimitRepository.deleteByPan(normalizedPan);
    }

    private LimitRecordResponse saveUpsertInternal(LimitUpsertRequest request, boolean allowCreate) {
        String pan = normalize(request.getPan());
        String seqNr = normalizeOrDefault(request.getSeqNr(), isoConfig.getDefaultSeqNr());
        if (pan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pan is required");
        }

        CardLimit existing = cardLimitRepository.findByPan(pan).orElse(null);
        if (existing == null && !allowCreate) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found for pan");
        }
        CardLimit entity = (existing != null) ? existing : new CardLimit();
        if (existing != null) {
            seqNr = normalizeOrDefault(existing.getSeqNr(), isoConfig.getDefaultSeqNr());
        }
        LocalDateTime now = LocalDateTime.now();

        CardLimitDTO dto = new CardLimitDTO();
        dto.setPan(pan);
        dto.setSeqNr(seqNr);
        dto.setLimits(request.getLimits());

        LimitEngineResult engineResult = limitCalculationService.buildLimitResult(dto);

        entity.setPan(pan);
        entity.setSeqNr(seqNr);
        entity.setIsoNr(request.getIsoNr() != null ? request.getIsoNr() : isoConfig.getDefaultNr());
        entity.setLimits(engineResult.getLimitPayload());
        entity.setLastUpdDate(now);
        entity.setLastUpdUser(normalizeOrDefault(request.getLastUpdUser(), isoConfig.getLastUpdUser()));
        cardLimitRepository.save(entity);

        limitExtraDataRepository.deleteByPanAndSeqNr(pan, seqNr);
        for (ResolvedLimitSegment segment : engineResult.getUnknownSegments()) {
            LimitExtraData extra = new LimitExtraData();
            extra.setPan(pan);
            extra.setSeqNr(seqNr);
            extra.setFieldName(segment.getFieldName());
            extra.setFieldValue(segment.getValue());
            extra.setSource("API");
            extra.setCreatedAt(now);
            limitExtraDataRepository.save(extra);
        }

        return mapToResponse(entity);
    }

    private LimitRecordResponse mapToResponse(CardLimit row) {
        LimitRecordResponse out = new LimitRecordResponse();
        out.setIsoNr(row.getIsoNr());
        out.setPan(row.getPan());
        out.setSeqNr(row.getSeqNr());
        out.setLimitsString(row.getLimits());
        out.setLastUpdUser(row.getLastUpdUser());
        out.setLastUpdDate(row.getLastUpdDate());
        out.setKnownLimits(new LinkedHashMap<>());

        Map<String, String> unknown = new LinkedHashMap<>();
        for (LimitExtraData v : limitExtraDataRepository.findByPanAndSeqNr(row.getPan(), row.getSeqNr())) {
            unknown.put(v.getFieldName(), v.getFieldValue());
        }
        out.setUnknownLimits(unknown);
        return out;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }
}
