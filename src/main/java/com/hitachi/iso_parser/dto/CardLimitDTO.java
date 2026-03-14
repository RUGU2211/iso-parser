package com.hitachi.iso_parser.dto;

import lombok.Data;

@Data
public class CardLimitDTO {

    private String pan;
    private String expiryDate;
    private String seqNr;

    private String cashLimit;
    private String goodsLimit;
    private String cardNotPresentLimit;
}
