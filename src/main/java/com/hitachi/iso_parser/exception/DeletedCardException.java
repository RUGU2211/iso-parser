package com.hitachi.iso_parser.exception;

import java.time.LocalDateTime;

public class DeletedCardException extends RuntimeException {

    private final String action;
    private final String deleteByUser;
    private final LocalDateTime deleteDateTime;
    private final String lastUpdatedUser;
    private final String pan;
    private final String seqNr;
    private final Integer issuerNr;

    public DeletedCardException(String action, String deleteByUser, LocalDateTime deleteDateTime,
            String lastUpdatedUser, String pan, String seqNr, Integer issuerNr) {
        super("Card is deleted and cannot be " + action);
        this.action = action;
        this.deleteByUser = deleteByUser;
        this.deleteDateTime = deleteDateTime;
        this.lastUpdatedUser = lastUpdatedUser;
        this.pan = pan;
        this.seqNr = seqNr;
        this.issuerNr = issuerNr;
    }

    public String getAction() {
        return action;
    }

    public String getDeleteByUser() {
        return deleteByUser;
    }

    public LocalDateTime getDeleteDateTime() {
        return deleteDateTime;
    }

    public String getLastUpdatedUser() {
        return lastUpdatedUser;
    }

    public String getPan() {
        return pan;
    }

    public String getSeqNr() {
        return seqNr;
    }

    public Integer getIssuerNr() {
        return issuerNr;
    }
}
