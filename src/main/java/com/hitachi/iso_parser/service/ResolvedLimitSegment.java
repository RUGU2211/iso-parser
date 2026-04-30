package com.hitachi.iso_parser.service;

public class ResolvedLimitSegment {

    private String fieldName;
    private String value;
    private Integer profileNr;
    private Integer ruleNr;
    private Integer priority;
    private String segmentPayload;
    private boolean known;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getProfileNr() {
        return profileNr;
    }

    public void setProfileNr(Integer profileNr) {
        this.profileNr = profileNr;
    }

    public Integer getRuleNr() {
        return ruleNr;
    }

    public void setRuleNr(Integer ruleNr) {
        this.ruleNr = ruleNr;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSegmentPayload() {
        return segmentPayload;
    }

    public void setSegmentPayload(String segmentPayload) {
        this.segmentPayload = segmentPayload;
    }

    public boolean isKnown() {
        return known;
    }

    public void setKnown(boolean known) {
        this.known = known;
    }
}
