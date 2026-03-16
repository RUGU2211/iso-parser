package com.hitachi.iso_parser.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.jpos.iso.ISOMsg;

/**
 * Formats ISO 8583 message for audit storage (MTI, Bitmap, Data Elements).
 */
public final class IsoFieldFormatter {

    private IsoFieldFormatter() {
    }

    /**
     * Formats ISOMsg to readable string (MTI, fields with values).
     * Example: 0600:\n   [LLVAR...] 002 [3538210000000026]\n   ...
     */
    public static String format(ISOMsg isoMsg) {
        if (isoMsg == null) {
            return "";
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
            isoMsg.dump(ps, "");
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Error formatting: " + e.getMessage();
        }
    }
}
