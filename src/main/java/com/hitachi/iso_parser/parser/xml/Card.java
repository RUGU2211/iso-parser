package com.hitachi.iso_parser.parser.xml;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;

@Data
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
}
