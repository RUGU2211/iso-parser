package com.hitachi.iso_parser.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * PostgreSQL table {@code pc_card_ext_lim_12_b}: one active row per (issuer_nr, pan, seq_nr).
 * <p>
 * <b>Mapping from inbound XML / ISO flow:</b>
 * <ul>
 *   <li>Limits resolved via {@code limit_master} (known types) → concatenated TLV payload → column {@code card_limits}.</li>
 *   <li>All received fields (known + unknown) → JSON map {@code fieldName → value} → column {@code total_data_received}.</li>
 *   <li>Audit: {@code created_date}, {@code last_updated_date}, {@code last_updated_user}; soft delete → {@code date_deleted}.</li>
 * </ul>
 */
@Entity
@Table(name = "pc_card_ext_lim_12_b")
public class CardLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ColumnDefault("13")
    @Column(name = "issuer_nr", nullable = false)
    private Integer issuerNr;

    @Column(nullable = false, length = 66)
    private String pan;

    @Column(name = "seq_nr", nullable = false, length = 3)
    private String seqNr;

    @ColumnDefault("''")
    @Column(name = "card_limits", nullable = false, columnDefinition = "TEXT")
    private String limits = "";

    @ColumnDefault("'{}'")
    @Column(name = "total_data_received", nullable = false, columnDefinition = "TEXT")
    private String totalDataReceived = "{}";

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_updated_date", nullable = false)
    private LocalDateTime lastUpdatedDate;

    @ColumnDefault("'sp'")
    @Column(name = "last_updated_user", nullable = false, length = 20)
    private String lastUpdatedUser;

    @Column(name = "date_deleted")
    private LocalDateTime dateDeleted;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (lastUpdatedDate == null) {
            lastUpdatedDate = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIssuerNr() {
        return issuerNr;
    }

    public void setIssuerNr(Integer issuerNr) {
        this.issuerNr = issuerNr;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getSeqNr() {
        return seqNr;
    }

    public void setSeqNr(String seqNr) {
        this.seqNr = seqNr;
    }

    /** TLV / encoded payload persisted in column {@code card_limits}. */
    public String getLimits() {
        return limits;
    }

    public void setLimits(String limits) {
        this.limits = limits;
    }

    public String getTotalDataReceived() {
        return totalDataReceived;
    }

    public void setTotalDataReceived(String totalDataReceived) {
        this.totalDataReceived = totalDataReceived != null ? totalDataReceived : "{}";
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getLastUpdatedUser() {
        return lastUpdatedUser;
    }

    public void setLastUpdatedUser(String lastUpdatedUser) {
        this.lastUpdatedUser = lastUpdatedUser;
    }

    public LocalDateTime getDateDeleted() {
        return dateDeleted;
    }

    public void setDateDeleted(LocalDateTime dateDeleted) {
        this.dateDeleted = dateDeleted;
    }
}
