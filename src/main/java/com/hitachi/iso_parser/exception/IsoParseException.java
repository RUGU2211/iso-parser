package com.hitachi.iso_parser.exception;

public class IsoParseException extends RuntimeException {

    public IsoParseException(String message) {
        super(message);
    }

    public IsoParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
