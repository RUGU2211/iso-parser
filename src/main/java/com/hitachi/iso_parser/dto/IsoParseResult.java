package com.hitachi.iso_parser.dto;

import org.jpos.iso.ISOMsg;

/**
 * Result of ISO8583 parsing - holds extracted XML and DE11 (System Trace Audit Number).
 */
public class IsoParseResult {

    private String xml;
    private String de11;
    private ISOMsg isoMsg;

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getDe11() {
        return de11;
    }

    public void setDe11(String de11) {
        this.de11 = de11;
    }

    public ISOMsg getIsoMsg() {
        return isoMsg;
    }

    public void setIsoMsg(ISOMsg isoMsg) {
        this.isoMsg = isoMsg;
    }
}
