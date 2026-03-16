package com.hitachi.iso_parser.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "iso_audit")
public class IsoAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_in", length = 8000)
    private String reqIn;

    @Column(name = "binary_hex", length = 8000)
    private String binaryHex;

    @Column(name = "iso_fields_formatted", length = 16000)
    private String isoFieldsFormatted;

    @Column(name = "resp_out", length = 4000)
    private String respOut;

    @Column(name = "de11", length = 20)
    private String de11;

    @Column(name = "pan", length = 50)
    private String pan;

    @Column(name = "expiry_date", length = 10)
    private String expiryDate;

    @Column(name = "seq_nr", length = 10)
    private String seqNr;

    @Column(name = "cash_limit", length = 50)
    private String cashLimit;

    @Column(name = "goods_limit", length = 50)
    private String goodsLimit;

    @Column(name = "card_not_present_limit", length = 50)
    private String cardNotPresentLimit;

    @Column(name = "de39", length = 10)
    private String de39;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public IsoAudit() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReqIn() {
        return reqIn;
    }

    public void setReqIn(String reqIn) {
        this.reqIn = reqIn;
    }

    public String getBinaryHex() {
        return binaryHex;
    }

    public void setBinaryHex(String binaryHex) {
        this.binaryHex = binaryHex;
    }

    public String getIsoFieldsFormatted() {
        return isoFieldsFormatted;
    }

    public void setIsoFieldsFormatted(String isoFieldsFormatted) {
        this.isoFieldsFormatted = isoFieldsFormatted;
    }

    public String getRespOut() {
        return respOut;
    }

    public void setRespOut(String respOut) {
        this.respOut = respOut;
    }

    public String getDe11() {
        return de11;
    }

    public void setDe11(String de11) {
        this.de11 = de11;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSeqNr() {
        return seqNr;
    }

    public void setSeqNr(String seqNr) {
        this.seqNr = seqNr;
    }

    public String getCashLimit() {
        return cashLimit;
    }

    public void setCashLimit(String cashLimit) {
        this.cashLimit = cashLimit;
    }

    public String getGoodsLimit() {
        return goodsLimit;
    }

    public void setGoodsLimit(String goodsLimit) {
        this.goodsLimit = goodsLimit;
    }

    public String getCardNotPresentLimit() {
        return cardNotPresentLimit;
    }

    public void setCardNotPresentLimit(String cardNotPresentLimit) {
        this.cardNotPresentLimit = cardNotPresentLimit;
    }

    public String getDe39() {
        return de39;
    }

    public void setDe39(String de39) {
        this.de39 = de39;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
