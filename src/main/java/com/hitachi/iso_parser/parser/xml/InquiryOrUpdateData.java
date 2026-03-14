package com.hitachi.iso_parser.parser.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "InquiryOrUpdateData")
public class InquiryOrUpdateData {

    @JacksonXmlProperty(localName = "Card")
    private Card card;
}
