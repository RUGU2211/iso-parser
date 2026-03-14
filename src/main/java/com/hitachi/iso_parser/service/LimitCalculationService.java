package com.hitachi.iso_parser.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hitachi.iso_parser.config.LimitConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.entity.LimitMaster;
import com.hitachi.iso_parser.repository.LimitMasterRepository;

@Service
public class LimitCalculationService {

    private LimitMasterRepository limitMasterRepository;
    private LimitConfig limitConfig;

    public LimitCalculationService(LimitMasterRepository limitMasterRepository, LimitConfig limitConfig) {
        this.limitMasterRepository = limitMasterRepository;
        this.limitConfig = limitConfig;
    }

    public String buildLimitString(CardLimitDTO dto) {
        List<LimitMaster> masters = limitMasterRepository.findAll();
        if (masters == null || masters.isEmpty()) {
            return buildFallbackLimitString(dto);
        }

        String posLimit = dto.getGoodsLimit();
        if (posLimit == null || posLimit.trim().isEmpty()) {
            posLimit = limitConfig.getDefaultValue();
        } else {
            posLimit = posLimit.trim();
        }

        String ecomLimit = dto.getCardNotPresentLimit();
        if (ecomLimit == null || ecomLimit.trim().isEmpty()) {
            ecomLimit = limitConfig.getDefaultValue();
        } else {
            ecomLimit = ecomLimit.trim();
        }

        String cashLimit = dto.getCashLimit();
        if (cashLimit == null || cashLimit.trim().isEmpty()) {
            cashLimit = limitConfig.getDefaultValue();
        } else {
            cashLimit = cashLimit.trim();
        }

        String result = limitConfig.getPrefix1() + limitConfig.getMax21();
        result = result + "|" + posLimit + "|" + limitConfig.getMax21();
        result = result + "|" + cashLimit + " " + limitConfig.getMax12();
        result = result + limitConfig.getPrefix2() + limitConfig.getMax21();
        result = result + "|" + ecomLimit + limitConfig.getMax21() + cashLimit + limitConfig.getMax12();

        return result;
    }

    private String buildFallbackLimitString(CardLimitDTO dto) {
        String cash = dto.getCashLimit();
        if (cash == null || cash.trim().isEmpty()) cash = limitConfig.getDefaultValue();
        else cash = cash.trim();

        String goods = dto.getGoodsLimit();
        if (goods == null || goods.trim().isEmpty()) goods = limitConfig.getDefaultValue();
        else goods = goods.trim();

        String ecom = dto.getCardNotPresentLimit();
        if (ecom == null || ecom.trim().isEmpty()) ecom = limitConfig.getDefaultValue();
        else ecom = ecom.trim();

        return cash + "|" + goods + "|" + ecom;
    }
}
