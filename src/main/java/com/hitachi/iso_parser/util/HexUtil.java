package com.hitachi.iso_parser.util;

import com.hitachi.iso_parser.exception.IsoParseException;

public class HexUtil {

    public static boolean isValidHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return false;
        }
        return hex.matches("^[0-9A-Fa-f]+$");
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.trim().isEmpty()) {
            throw new IsoParseException("HEX string cannot be null or empty");
        }
        String normalized = hex.trim().replaceAll("\\s+", "");
        if (!isValidHex(normalized)) {
            throw new IsoParseException("Invalid HEX format");
        }

        int len = normalized.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i = i + 2) {
            int high = Character.digit(normalized.charAt(i), 16);
            int low = Character.digit(normalized.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IsoParseException("Invalid HEX at position " + i);
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }
}
