package com.hitachi.iso_parser.exception;

public class XmlParseException extends RuntimeException {

    public XmlParseException(String message) {
        super(message);
    }

    public XmlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
