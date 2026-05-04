package com.hitachi.iso_parser.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.LimitRecordResponse;
import com.hitachi.iso_parser.dto.LimitUpsertRequest;
import com.hitachi.iso_parser.entity.IsoAudit;
import com.hitachi.iso_parser.repository.IsoAuditRepository;

/**
 * Persists limit CRUD and read operations into {@code iso_audit} in a separate transaction
 * so failures in the business transaction still leave an audit trail when possible.
 */
@Service
public class CrudAuditService {

    private static final int MAX_DETAIL = 15500;
    private static final int MAX_REQ_IN = 7900;

    private final IsoAuditRepository isoAuditRepository;
    private final IsoConfig isoConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CrudAuditService(IsoAuditRepository isoAuditRepository, IsoConfig isoConfig) {
        this.isoAuditRepository = isoAuditRepository;
        this.isoConfig = isoConfig;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLimitGet(String pan, String requestedByUser, List<LimitRecordResponse> rows, long durationMs, boolean success, String errorMessage) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("resource", "LIMIT");
        d.put("operation", "GET");
        d.put("requestedByUser", requestedByUser);
        d.put("rowCount", rows != null ? rows.size() : 0);
        if (rows != null && !rows.isEmpty()) {
            d.put("issuerSeqPairs", rows.stream()
                    .map(r -> r.getIssuerNr() + "/" + r.getSeqNr())
                    .collect(Collectors.toList()));
        }
        persist("LIMIT_GET", pan, null, durationMs, success, errorMessage, d);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLimitCreate(String pan, LimitUpsertRequest request, LimitRecordResponse response, long durationMs,
            boolean success, String errorMessage) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("resource", "LIMIT");
        d.put("operation", "CREATE");
        d.put("request", summarizeRequest(request));
        if (response != null) {
            d.put("response", summarizeResponse(response));
        }
        persist("LIMIT_CREATE", pan, request != null ? request.getSeqNr() : null, durationMs, success, errorMessage, d);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLimitUpdate(String pan, LimitUpsertRequest request, LimitRecordResponse response, long durationMs,
            boolean success, String errorMessage) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("resource", "LIMIT");
        d.put("operation", "UPDATE");
        d.put("request", summarizeRequest(request));
        if (response != null) {
            d.put("response", summarizeResponse(response));
        }
        persist("LIMIT_UPDATE", pan, request != null ? request.getSeqNr() : null, durationMs, success, errorMessage, d);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLimitDelete(String pan, String requestedByUser, int rowsDeleted, String deletedByUser, LocalDateTime deletedAt, long durationMs,
            boolean success, String errorMessage) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("resource", "LIMIT");
        d.put("operation", "DELETE");
        d.put("requestedByUser", requestedByUser);
        d.put("rowsDeleted", rowsDeleted);
        d.put("deletedByUser", deletedByUser);
        d.put("deletedAt", deletedAt);
        persist("LIMIT_DELETE", pan, null, durationMs, success, errorMessage, d);
    }

    private void persist(String apiOperation, String pan, String seqNr, long durationMs, boolean success,
            String errorMessage, Map<String, Object> detail) {
        IsoAudit audit = new IsoAudit();
        audit.setCreatedAt(LocalDateTime.now());
        audit.setRequestId(UUID.randomUUID().toString());
        audit.setApiOperation(apiOperation);
        audit.setPan(trunc(pan, 50));
        audit.setSeqNr(trunc(seqNr, 10));
        audit.setProcessingTimeMs(durationMs);
        audit.setDe39(success ? isoConfig.getDe39Success() : isoConfig.getDe39Failed());
        audit.setErrorMessage(trunc(errorMessage, 2000));
        audit.setReqIn(trunc("RESOURCE=LIMIT|" + apiOperation + "|pan=" + (pan != null ? pan : ""), MAX_REQ_IN));
        audit.setRespOut(success ? "OK" : "FAIL");
        try {
            audit.setApiDetail(trunc(objectMapper.writeValueAsString(detail), MAX_DETAIL));
        } catch (JsonProcessingException e) {
            audit.setApiDetail("{\"error\":\"json_serialize_failed\"}");
        }
        isoAuditRepository.save(audit);
    }

    private Map<String, Object> summarizeRequest(LimitUpsertRequest r) {
        if (r == null) {
            return Map.of();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issuerNr", r.getIssuerNr());
        m.put("seqNr", r.getSeqNr());
        m.put("lastUpdUser", r.getLastUpdUser());
        if (r.getLimits() != null && !r.getLimits().isEmpty()) {
            Map<String, String> slim = new LinkedHashMap<>();
            r.getLimits().forEach((k, v) -> slim.put(k, trunc(v, 48)));
            m.put("limits", slim);
        }
        return m;
    }

    private Map<String, Object> summarizeResponse(LimitRecordResponse r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issuerNr", r.getIssuerNr());
        m.put("seqNr", r.getSeqNr());
        m.put("lastUpdUser", r.getLastUpdUser());
        m.put("limitsStringLen", r.getLimitsString() != null ? r.getLimitsString().length() : 0);
        m.put("unknownLimitKeys", r.getUnknownLimits() != null ? r.getUnknownLimits().keySet() : List.of());
        return m;
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
