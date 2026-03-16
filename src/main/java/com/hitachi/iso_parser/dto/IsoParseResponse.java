package com.hitachi.iso_parser.dto;

/**
 * API response with DE39 (Response Code): 00=success, 01=failed.
 */
public class IsoParseResponse {

    private boolean success;
    private String message;
    private String de39;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDe39() {
        return de39;
    }

    public void setDe39(String de39) {
        this.de39 = de39;
    }
}
