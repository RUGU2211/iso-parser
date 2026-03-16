package com.hitachi.iso_parser.parser;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.exception.XmlParseException;
import com.hitachi.iso_parser.parser.xml.Card;
import com.hitachi.iso_parser.parser.xml.InquiryOrUpdateData;
import com.hitachi.iso_parser.parser.xml.LimitField;

@Component
public class XmlLimitParser {

    private XmlMapper xmlMapper = new XmlMapper();
    private IsoConfig isoConfig;

    public XmlLimitParser(IsoConfig isoConfig) {
        this.isoConfig = isoConfig;
    }

    public CardLimitDTO parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new XmlParseException("XML content cannot be null or empty");
        }

        String toParse = xml.trim();
        // Strip any leading length/prefix (e.g. "295", "3295", "Postilion:InquiryOrUpdateData3295")
        int firstLt = toParse.indexOf('<');
        if (firstLt > 0) {
            toParse = toParse.substring(firstLt);
        } else if (firstLt < 0) {
            throw new XmlParseException("No XML content found (expected '<' in field 127.022; got: " + (toParse.length() > 50 ? toParse.substring(0, 50) + "..." : toParse) + ")");
        }
        int start = toParse.indexOf("<InquiryOrUpdateData");
        if (start >= 0) {
            int end = toParse.indexOf("</InquiryOrUpdateData>");
            if (end > start) {
                toParse = toParse.substring(start, end + "</InquiryOrUpdateData>".length());
            }
        }
        start = toParse.indexOf("<Postilion:InquiryOrUpdateData");
        if (start >= 0) {
            int end = toParse.indexOf("</Postilion:InquiryOrUpdateData>");
            if (end > start) {
                String inner = toParse.substring(start, end + "</Postilion:InquiryOrUpdateData>".length());
                toParse = inner.replace("Postilion:", "").replace(":Postilion", "");
            }
        }

        try {
            InquiryOrUpdateData root = xmlMapper.readValue(toParse, InquiryOrUpdateData.class);
            if (root == null || root.getCard() == null) {
                throw new XmlParseException("XML must contain Card element");
            }
            return mapToDto(root.getCard());
        } catch (XmlParseException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlParseException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    private CardLimitDTO mapToDto(Card card) {
        CardLimitDTO dto = new CardLimitDTO();

        if (card.getPan() == null || card.getPan().trim().isEmpty()) {
            throw new XmlParseException("PAN is required");
        }
        dto.setPan(card.getPan().trim());

        if (card.getExpiryDate() == null || card.getExpiryDate().trim().isEmpty()) {
            throw new XmlParseException("ExpiryDate is required");
        }
        dto.setExpiryDate(card.getExpiryDate().trim());

        if (card.getSeqNr() != null && !card.getSeqNr().trim().isEmpty()) {
            dto.setSeqNr(card.getSeqNr().trim());
        } else {
            dto.setSeqNr(isoConfig.getDefaultSeqNr());
        }

        if (card.getFields() != null) {
            for (LimitField field : card.getFields()) {
                if (field == null || field.getName() == null) continue;

                String name = field.getName().trim();
                String value = "";
                if (field.getValue() != null) {
                    value = field.getValue().trim();
                }

                if (name.equals("cash_limit")) {
                    dto.setCashLimit(value);
                } else if (name.equals("goods_limit")) {
                    dto.setGoodsLimit(value);
                } else if (name.equals("card_not_present_limit")) {
                    dto.setCardNotPresentLimit(value);
                }
            }
        }

        return dto;
    }
}
