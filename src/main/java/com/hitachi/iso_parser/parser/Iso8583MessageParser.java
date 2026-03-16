package com.hitachi.iso_parser.parser;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.iso.packager.ISO87APackager;
import org.jpos.iso.packager.PostPackager;
import org.springframework.stereotype.Component;

import com.hitachi.iso_parser.dto.IsoParseResult;
import com.hitachi.iso_parser.exception.IsoParseException;
import com.hitachi.iso_parser.util.HexUtil;

@Component
public class Iso8583MessageParser {

    /**
     * Parse ISO message and extract XML + DE11 (System Trace Audit Number).
     */
    public IsoParseResult parse(String hexMessage) {
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

        try {
            IsoParseResult result = parseWithPostPackager(messageBytes);
            if (result != null && result.getXml() != null && !result.getXml().trim().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            // ignore, try next
        }

        try {
            IsoParseResult result = parseWithIso87Packager(messageBytes);
            if (result != null && result.getXml() != null && !result.getXml().trim().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            throw new IsoParseException("Could not extract XML from field 127 or 127.022");
        }

        throw new IsoParseException("Could not extract XML from field 127 or 127.022");
    }

    /**
     * @deprecated Use {@link #parse(String)} instead. Kept for backward compatibility.
     */
    public String extractXml(String hexMessage) {
        IsoParseResult result = parse(hexMessage);
        return result.getXml();
    }

    private IsoParseResult parseWithPostPackager(byte[] messageBytes) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(new PostPackager());
        isoMsg.unpack(messageBytes);

        String xml = getFieldValue(isoMsg, "127.22");
        if (xml == null) xml = getFieldValue(isoMsg, "127.022");
        if (xml == null) xml = getFieldValue(isoMsg, "127");

        IsoParseResult result = new IsoParseResult();
        result.setIsoMsg(isoMsg);
        result.setXml(xml);
        result.setDe11(getFieldValue(isoMsg, "11"));
        return result;
    }

    private IsoParseResult parseWithIso87Packager(byte[] messageBytes) throws ISOException {
        ISOMsg isoMsg = new ISOMsg();
        isoMsg.setPackager(new ISO87APackager());
        isoMsg.unpack(messageBytes);

        IsoParseResult result = new IsoParseResult();
        result.setIsoMsg(isoMsg);
        result.setXml(getFieldValue(isoMsg, "127"));
        result.setDe11(getFieldValue(isoMsg, "11"));
        return result;
    }

    private String getFieldValue(ISOMsg msg, String fieldKey) {
        try {
            String val = msg.getString(fieldKey);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        } catch (Exception e) {
            // try numeric field
        }
        try {
            int fieldNum = Integer.parseInt(fieldKey);
            String val = msg.getString(fieldNum);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
