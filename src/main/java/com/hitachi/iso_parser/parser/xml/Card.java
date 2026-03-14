package com.hitachi.iso_parser.parser.xml;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Card {

    @JacksonXmlProperty(localName = "PAN")
    private String pan;

    @JacksonXmlProperty(localName = "ExpiryDate")
    private String expiryDate;

    @JacksonXmlProperty(localName = "SeqNr")
    private String seqNr;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Field")
    private List<LimitField> fields = new ArrayList<>();

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

    public List<LimitField> getFields() {
        return fields;
    }

    public void setFields(List<LimitField> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }
}
