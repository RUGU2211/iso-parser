package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.dto.LimitDeleteResponse;
import com.hitachi.iso_parser.dto.LimitRecordResponse;
import com.hitachi.iso_parser.dto.LimitUpsertRequest;
import com.hitachi.iso_parser.entity.CardLimit;
import com.hitachi.iso_parser.entity.LimitMaster;
import com.hitachi.iso_parser.exception.DeletedCardException;
import com.hitachi.iso_parser.repository.CardLimitRepository;
import com.hitachi.iso_parser.repository.LimitMasterRepository;
import com.hitachi.iso_parser.util.ExtraLimitsJsonMapper;

@Service
public class CardLimitCrudService {

    private final CardLimitRepository cardLimitRepository;
    private final LimitCalculationService limitCalculationService;
    private final IsoConfig isoConfig;
    private final ExtraLimitsJsonMapper extraLimitsJsonMapper;
    private final CrudAuditService crudAuditService;
    private final LimitMasterRepository limitMasterRepository;

    public CardLimitCrudService(CardLimitRepository cardLimitRepository,
            LimitCalculationService limitCalculationService,
            IsoConfig isoConfig,
            ExtraLimitsJsonMapper extraLimitsJsonMapper,
            CrudAuditService crudAuditService,
            LimitMasterRepository limitMasterRepository) {
        this.cardLimitRepository = cardLimitRepository;
        this.limitCalculationService = limitCalculationService;
        this.isoConfig = isoConfig;
        this.extraLimitsJsonMapper = extraLimitsJsonMapper;
        this.crudAuditService = crudAuditService;
        this.limitMasterRepository = limitMasterRepository;
    }

    public List<LimitRecordResponse> getByPan(String pan) {
        long startNs = System.nanoTime();
        String normalized = normalize(pan);
        String requester = trimUser(normalizeOrDefault(isoConfig.getLastUpdUser(), "unknown"));
        try {
            List<CardLimit> rows = cardLimitRepository.findAllByPanAndDateDeletedIsNullOrderBySeqNrAsc(normalized);
            if (rows.isEmpty()) {
                CardLimit deleted = cardLimitRepository
                        .findFirstByPanAndDateDeletedIsNotNullOrderByDateDeletedDesc(normalized)
                        .orElse(null);
                if (deleted != null) {
                    throw deletedCardException("requested", deleted);
                }
            }
            List<LimitRecordResponse> out = rows.stream().map(this::mapToResponse).toList();
            crudAuditService.logLimitGet(normalized, requester, out, elapsedMs(startNs), true, null);
            return out;
        } catch (ResponseStatusException e) {
            crudAuditService.logLimitGet(normalized, requester, List.of(), elapsedMs(startNs), false, statusExceptionDetail(e));
            throw e;
        } catch (RuntimeException e) {
            crudAuditService.logLimitGet(normalized, requester, List.of(), elapsedMs(startNs), false, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public LimitRecordResponse create(String panPath, LimitUpsertRequest request) {
        long startNs = System.nanoTime();
        String pan = normalize(panPath);
        try {
            if (pan.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pan is required");
            }
            Integer issuerNr = resolveIssuerNr(request);
            String seqNr = normalizeOrDefault(request.getSeqNr(), isoConfig.getDefaultSeqNr());
            if (cardLimitRepository.existsByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(issuerNr, pan, seqNr)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Record already exists for issuer_nr, pan, seq_nr. Use PUT for upsert.");
            }
            request.setPan(pan);
            SaveResult result = saveUpsertInternal(request, false);
            crudAuditService.logLimitCreate(pan, request, result.response(), elapsedMs(startNs), true, null);
            return result.response();
        } catch (ResponseStatusException e) {
            crudAuditService.logLimitCreate(pan, request, null, elapsedMs(startNs), false, statusExceptionDetail(e));
            throw e;
        } catch (RuntimeException e) {
            crudAuditService.logLimitCreate(pan, request, null, elapsedMs(startNs), false, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public LimitRecordResponse upsert(String panPath, LimitUpsertRequest request) {
        long startNs = System.nanoTime();
        request.setPan(normalize(panPath));
        String pan = normalize(request.getPan());
        try {
            SaveResult result = saveUpsertInternal(request, true);
            if (result.created()) {
                crudAuditService.logLimitCreate(pan, request, result.response(), elapsedMs(startNs), true, null);
            } else {
                crudAuditService.logLimitUpdate(pan, request, result.response(), elapsedMs(startNs), true, null);
            }
            return result.response();
        } catch (ResponseStatusException e) {
            crudAuditService.logLimitUpdate(pan, request, null, elapsedMs(startNs), false, statusExceptionDetail(e));
            throw e;
        } catch (RuntimeException e) {
            crudAuditService.logLimitUpdate(pan, request, null, elapsedMs(startNs), false, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public LimitDeleteResponse delete(String pan) {
        long startNs = System.nanoTime();
        String normalizedPan = normalize(pan);
        String requester = trimUser(normalizeOrDefault(isoConfig.getLastUpdUser(), "unknown"));
        try {
            List<CardLimit> existing = cardLimitRepository.findAllByPanAndDateDeletedIsNullOrderBySeqNrAsc(normalizedPan);
            if (existing.isEmpty()) {
                crudAuditService.logLimitDelete(normalizedPan, requester, 0, null, null, elapsedMs(startNs), false, "Record not found for pan");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found for pan");
            }
            int n = existing.size();
            String deletedByUser = requester;
            LocalDateTime deletedAt = LocalDateTime.now();
            cardLimitRepository.softDeleteByPanWithUser(normalizedPan, deletedAt, deletedByUser);
            crudAuditService.logLimitDelete(normalizedPan, requester, n, deletedByUser, deletedAt, elapsedMs(startNs), true, null);
            LimitDeleteResponse out = new LimitDeleteResponse();
            out.setSuccess(true);
            out.setMessage("Card limits deleted successfully");
            out.setPan(normalizedPan);
            out.setDeletedByUser(deletedByUser);
            out.setDeletedRecords(n);
            out.setDeletedAt(deletedAt);
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            crudAuditService.logLimitDelete(normalizedPan, requester, 0, null, null, elapsedMs(startNs), false, e.getMessage());
            throw e;
        }
    }

    private SaveResult saveUpsertInternal(LimitUpsertRequest request, boolean allowCreate) {
        String pan = normalize(request.getPan());
        Integer issuerNr = resolveIssuerNr(request);
        String seqNr = normalizeOrDefault(request.getSeqNr(), isoConfig.getDefaultSeqNr());
        if (pan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pan is required");
        }

        CardLimit existing = cardLimitRepository
                .findByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(issuerNr, pan, seqNr)
                .orElse(null);
        if (existing == null && !allowCreate) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found for issuer_nr, pan, seq_nr");
        }
        boolean created = existing == null;
        LocalDateTime now = LocalDateTime.now();

        CardLimitDTO dto = new CardLimitDTO();
        dto.setPan(pan);
        dto.setSeqNr(seqNr);
        dto.setLimits(request.getLimits());

        LimitEngineResult engineResult = limitCalculationService.buildLimitResult(dto);

        Map<String, String> extra = new LinkedHashMap<>();
        for (ResolvedLimitSegment segment : engineResult.getUnknownSegments()) {
            extra.put(segment.getFieldName(), segment.getValue());
        }

        CardLimit entity = existing != null ? existing : new CardLimit();
        if (existing == null) {
            entity.setCreatedDate(now);
        }
        entity.setIssuerNr(issuerNr);
        entity.setPan(pan);
        entity.setSeqNr(seqNr);
        entity.setLimits(engineResult.getLimitPayload());
        entity.setLimitExtraData(extraLimitsJsonMapper.toJson(extra));
        entity.setLastUpdatedDate(now);
        entity.setLastUpdatedUser(trimUser(normalizeOrDefault(request.getLastUpdUser(), isoConfig.getLastUpdUser())));
        entity.setDateDeleted(null);
        cardLimitRepository.save(entity);

        return new SaveResult(created, mapToResponse(entity));
    }

    private Integer resolveIssuerNr(LimitUpsertRequest request) {
        if (request.getIssuerNr() != null) {
            return request.getIssuerNr();
        }
        return isoConfig.getDefaultNr();
    }

    private LimitRecordResponse mapToResponse(CardLimit row) {
        LimitRecordResponse out = new LimitRecordResponse();
        out.setIssuerNr(row.getIssuerNr());
        out.setPan(row.getPan());
        out.setSeqNr(row.getSeqNr());
        out.setLimitsString(row.getLimits());
        out.setLastUpdUser(row.getLastUpdatedUser());
        out.setLastUpdDate(row.getLastUpdatedDate());
        out.setKnownLimits(extractKnownLimits(row.getLimits()));
        out.setUnknownLimits(extraLimitsJsonMapper.unmodifiableView(extraLimitsJsonMapper.fromJson(row.getLimitExtraData())));
        return out;
    }

    private Map<String, String> extractKnownLimits(String payload) {
        Map<String, String> out = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) {
            return out;
        }
        Map<Integer, String> ruleToLimitName = loadRuleToLimitNameMap();
        int idx = 0;
        while (idx + 8 < payload.length()) {
            if (!payload.startsWith("12", idx)) {
                idx++;
                continue;
            }
            if (idx + 5 >= payload.length() || payload.charAt(idx + 4) != '2') {
                idx++;
                continue;
            }
            int outerLenDigits = resolveLengthDigits(payload, idx + 5, payload.length() - (idx + 5));
            if (outerLenDigits < 0) {
                idx++;
                continue;
            }
            int outerLen = Integer.parseInt(payload.substring(idx + 5, idx + 5 + outerLenDigits));
            int innerStart = idx + 5 + outerLenDigits;
            int outerEnd = innerStart + outerLen;
            if (outerEnd > payload.length() || !payload.startsWith("12", innerStart)) {
                idx++;
                continue;
            }
            if (innerStart + 5 >= outerEnd || payload.charAt(innerStart + 4) != '2') {
                idx = outerEnd;
                continue;
            }
            int innerLenDigits = resolveLengthDigits(payload, innerStart + 5, outerEnd - (innerStart + 5));
            if (innerLenDigits < 0) {
                idx = outerEnd;
                continue;
            }
            int innerLen = Integer.parseInt(payload.substring(innerStart + 5, innerStart + 5 + innerLenDigits));
            int valueStart = innerStart + 5 + innerLenDigits;
            int valueEnd = valueStart + innerLen;
            if (valueEnd > outerEnd || valueStart >= valueEnd) {
                idx = outerEnd;
                continue;
            }
            String rule = payload.substring(innerStart + 2, innerStart + 4);
            String values = payload.substring(valueStart, valueEnd);
            String[] tokens = values.split("\\|");
            if (tokens.length >= 3) {
                int ruleNo = Integer.parseInt(rule);
                String key = ruleToLimitName.getOrDefault(ruleNo, "rule_" + rule);
                out.put(key, tokens[2]);
            }
            idx = outerEnd;
        }
        return out;
    }

    private int resolveLengthDigits(String payload, int lenPos, int maxRemaining) {
        if (lenPos + 2 <= payload.length()) {
            int len2 = parseLen(payload, lenPos, 2);
            if (len2 >= 0 && len2 <= maxRemaining) {
                return 2;
            }
        }
        if (lenPos + 3 <= payload.length()) {
            int len3 = parseLen(payload, lenPos, 3);
            if (len3 >= 0 && len3 <= maxRemaining) {
                return 3;
            }
        }
        return -1;
    }

    private int parseLen(String payload, int start, int digits) {
        try {
            return Integer.parseInt(payload.substring(start, start + digits));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private Map<Integer, String> loadRuleToLimitNameMap() {
        List<LimitMaster> masters = limitMasterRepository.findByIsActiveTrueOrderByPriorityAsc();
        Map<Integer, String> map = new HashMap<>();
        for (LimitMaster m : masters) {
            if (m.getLimitRuleNr() != null && m.getLimitName() != null) {
                map.putIfAbsent(m.getLimitRuleNr(), m.getLimitName());
            }
        }
        return map;
    }

    private String trimUser(String value) {
        if (value.length() <= 20) {
            return value;
        }
        return value.substring(0, 20);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private static String statusExceptionDetail(ResponseStatusException e) {
        String r = e.getReason();
        if (r != null && !r.isEmpty()) {
            return r;
        }
        return e.getStatusCode().toString();
    }

    private DeletedCardException deletedCardException(String action, CardLimit deleted) {
        return new DeletedCardException(
                action,
                valueOrUnknown(deleted.getLastUpdatedUser()),
                deleted.getDateDeleted(),
                valueOrUnknown(deleted.getLastUpdatedUser()),
                valueOrUnknown(deleted.getPan()),
                valueOrUnknown(deleted.getSeqNr()),
                deleted.getIssuerNr());
    }

    private String valueOrUnknown(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }

    private record SaveResult(boolean created, LimitRecordResponse response) {
    }
}
