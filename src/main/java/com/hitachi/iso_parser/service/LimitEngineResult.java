package com.hitachi.iso_parser.service;

import java.util.ArrayList;
import java.util.List;

public class LimitEngineResult {

    private String limitPayload;
    private List<ResolvedLimitSegment> knownSegments = new ArrayList<>();
    private List<ResolvedLimitSegment> unknownSegments = new ArrayList<>();

    public String getLimitPayload() {
        return limitPayload;
    }

    public void setLimitPayload(String limitPayload) {
        this.limitPayload = limitPayload;
    }

    public List<ResolvedLimitSegment> getKnownSegments() {
        return knownSegments;
    }

    public void setKnownSegments(List<ResolvedLimitSegment> knownSegments) {
        this.knownSegments = knownSegments;
    }

    public List<ResolvedLimitSegment> getUnknownSegments() {
        return unknownSegments;
    }

    public void setUnknownSegments(List<ResolvedLimitSegment> unknownSegments) {
        this.unknownSegments = unknownSegments;
    }
}
