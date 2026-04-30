package com.hitachi.iso_parser.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class LimitUpsertRequest {

    private Integer isoNr;
    private String pan;
    private String seqNr;
    private String lastUpdUser;
    private Map<String, String> limits = new LinkedHashMap<>();

    public Integer getIsoNr() {
        return isoNr;
    }

    public void setIsoNr(Integer isoNr) {
        this.isoNr = isoNr;
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

    public String getLastUpdUser() {
        return lastUpdUser;
    }

    public void setLastUpdUser(String lastUpdUser) {
        this.lastUpdUser = lastUpdUser;
    }

    public Map<String, String> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, String> limits) {
        this.limits = (limits != null) ? limits : new LinkedHashMap<>();
    }
}
