package com.hitachi.iso_parser.dto;

public class CardLimitDTO {

    private String pan;
    private String expiryDate;
    private String seqNr;

    private String cashLimit;
    private String goodsLimit;
    private String cardNotPresentLimit;

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
}
