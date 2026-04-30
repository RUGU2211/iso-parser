package com.hitachi.iso_parser.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class CardLimitDTO {

    private String pan;
    private String expiryDate;
    private String seqNr;

    private Map<String, String> limits = new LinkedHashMap<>();

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSeqNr() {
        return seqNr;
    }

    public void setSeqNr(String seqNr) {
        this.seqNr = seqNr;
    }

    public Map<String, String> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, String> limits) {
        this.limits = (limits != null) ? limits : new LinkedHashMap<>();
    }

    public void putLimit(String name, String value) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        limits.put(name.trim(), value != null ? value.trim() : "");
    }

    public String getLimitValue(String name) {
        if (name == null) {
            return null;
        }
        return limits.get(name);
    }

    public boolean hasLimit(String name) {
        return name != null && limits.containsKey(name);
    }
}
