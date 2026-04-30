package com.hitachi.iso_parser.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.exception.XmlParseException;

class XmlLimitParserTest {

    private XmlLimitParser parser;

    @BeforeEach
    void setUp() {
        IsoConfig config = new IsoConfig();
        config.setDefaultSeqNr("001");
        parser = new XmlLimitParser(config);
    }

    @Test
    void parse_shouldCaptureDynamicFieldsAndDefaultSeq() {
        String xml = "<InquiryOrUpdateData><Card><PAN>3538210000000026</PAN><ExpiryDate>3005</ExpiryDate>"
                + "<Field Name=\"goods_limit\">80000</Field>"
                + "<Field Name=\"upi_limit\">12000</Field></Card></InquiryOrUpdateData>";

        CardLimitDTO dto = parser.parse(xml);

        assertEquals("3538210000000026", dto.getPan());
        assertEquals("3005", dto.getExpiryDate());
        assertEquals("001", dto.getSeqNr());
        assertEquals("80000", dto.getLimitValue("goods_limit"));
        assertEquals("12000", dto.getLimitValue("upi_limit"));
    }

    @Test
    void parse_shouldRejectNonNumericLimitValues() {
        String xml = "<InquiryOrUpdateData><Card><PAN>3538210000000026</PAN><ExpiryDate>3005</ExpiryDate>"
                + "<Field Name=\"goods_limit\">8O000</Field></Card></InquiryOrUpdateData>";

        assertThrows(XmlParseException.class, () -> parser.parse(xml));
    }
}
