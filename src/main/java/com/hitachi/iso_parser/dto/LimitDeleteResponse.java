package com.hitachi.iso_parser.dto;

import java.time.LocalDateTime;

public class LimitDeleteResponse {

    private boolean success;
    private String message;
    private String pan;
    private String deletedByUser;
    private int deletedRecords;
    private LocalDateTime deletedAt;

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

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getDeletedByUser() {
        return deletedByUser;
    }

    public void setDeletedByUser(String deletedByUser) {
        this.deletedByUser = deletedByUser;
    }

    public int getDeletedRecords() {
        return deletedRecords;
    }

    public void setDeletedRecords(int deletedRecords) {
        this.deletedRecords = deletedRecords;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
