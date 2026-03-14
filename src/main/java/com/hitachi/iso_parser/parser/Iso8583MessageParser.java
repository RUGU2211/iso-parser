package com.hitachi.iso_parser.parser;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.iso.packager.ISO87APackager;
import org.jpos.iso.packager.PostPackager;
import org.springframework.stereotype.Component;

import com.hitachi.iso_parser.exception.IsoParseException;
import com.hitachi.iso_parser.util.HexUtil;

@Component
public class Iso8583MessageParser {

    public String extractXml(String hexMessage) {
        if (hexMessage == null || hexMessage.trim().isEmpty()) {
            throw new IsoParseException("ISO message cannot be null or empty");
        }

        String normalizedHex = hexMessage.trim().replaceAll("\\s+", "");
        if (!HexUtil.isValidHex(normalizedHex)) {
            throw new IsoParseException("Invalid HEX format in ISO message");
        }

        byte[] messageBytes = HexUtil.hexToBytes(normalizedHex);
        if (messageBytes.length == 0) {
            throw new IsoParseException("Empty message after HEX conversion");
        }

        // try PostPackager first
        try {
            String xml = extractWithPostPackager(messageBytes);
            if (xml != null && !xml.trim().isEmpty()) {
                return xml.trim();
            }
        } catch (Exception e) {
            // ignore, try next
        }

        // fallback to ISO87
        try {
            String xml = extractWithIso87Packager(messageBytes);
            if (xml != null && !xml.trim().isEmpty()) {
                return xml.trim();
            }
        } catch (Exception e) {
            throw new IsoParseException("Could not extract XML from field 127 or 127.022");
        }

        throw new IsoParseException("Could not extract XML from field 127 or 127.022");
    }

    private String extractWithPostPackager(byte[] messageBytes) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(new PostPackager());
        isoMsg.unpack(messageBytes);

        String xml = getFieldValue(isoMsg, "127.22");
        if (xml == null) {
            xml = getFieldValue(isoMsg, "127.022");
        }
        if (xml == null) {
            xml = getFieldValue(isoMsg, "127");
        }
        return xml;
    }

    private String extractWithIso87Packager(byte[] messageBytes) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(new ISO87APackager());
        isoMsg.unpack(messageBytes);
        return getFieldValue(isoMsg, "127");
    }

    private String getFieldValue(ISOMsg msg, String fieldKey) {
        try {
            String val = msg.getString(fieldKey);
            if (val != null && !val.trim().isEmpty()) {
                return val;
            }
        } catch (Exception e) {
            if ("127".equals(fieldKey)) {
                try {
                    String val = msg.getString(127);
                    if (val != null && !val.trim().isEmpty()) {
                        return val;
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return null;
    }
}
