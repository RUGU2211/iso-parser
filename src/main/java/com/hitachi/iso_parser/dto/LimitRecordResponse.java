package com.hitachi.iso_parser.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LimitRecordResponse {

    @JsonProperty("issuerNr")
    @JsonAlias({ "isoNr" })
    private Integer issuerNr;

    private String pan;
    private String seqNr;
    @JsonIgnore
    private String limitsString;
    private String lastUpdUser;
    private LocalDateTime lastUpdDate;
    private Map<String, String> knownLimits = new LinkedHashMap<>();
    private Map<String, String> unknownLimits = new LinkedHashMap<>();

    public Integer getIssuerNr() {
        return issuerNr;
    }

    public void setIssuerNr(Integer issuerNr) {
        this.issuerNr = issuerNr;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getSeqNr() {
        return seqNr;
    }

    public void setSeqNr(String seqNr) {
        this.seqNr = seqNr;
    }

    public String getLimitsString() {
        return limitsString;
    }

    public void setLimitsString(String limitsString) {
        this.limitsString = limitsString;
    }

    public String getLastUpdUser() {
        return lastUpdUser;
    }

    public void setLastUpdUser(String lastUpdUser) {
        this.lastUpdUser = lastUpdUser;
    }

    public LocalDateTime getLastUpdDate() {
        return lastUpdDate;
    }

    public void setLastUpdDate(LocalDateTime lastUpdDate) {
        this.lastUpdDate = lastUpdDate;
    }

    public Map<String, String> getKnownLimits() {
        return knownLimits;
    }

    public void setKnownLimits(Map<String, String> knownLimits) {
        this.knownLimits = (knownLimits != null) ? knownLimits : new LinkedHashMap<>();
    }

    public Map<String, String> getUnknownLimits() {
        return unknownLimits;
    }

    public void setUnknownLimits(Map<String, String> unknownLimits) {
        this.unknownLimits = (unknownLimits != null) ? unknownLimits : new LinkedHashMap<>();
    }
}
