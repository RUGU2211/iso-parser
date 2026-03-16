package com.hitachi.iso_parser.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hitachi.iso_parser.config.LimitConfig;
import com.hitachi.iso_parser.dto.CardLimitDTO;
import com.hitachi.iso_parser.entity.LimitMaster;
import com.hitachi.iso_parser.repository.LimitMasterRepository;

/**
 * Builds limit string dynamically with TLV-style encoding.
 * Same formula applies to a1, a2, a3 - only profile_nr, rule_nr (from limit_master) and value differ.
 * <p>
 * Format for each part (a1, a2, a3):
 * 1 2 {profile_nr} 2 {data_len} [inner_data]
 * Where inner_data = 1 2 {rule_nr} 2 {limit_values_len} [limit_values]
 * And limit_values = f|f|val|f|f|val|f
 * <p>
 * a1: goods_limit (profile=42, rule=52), a2: card_not_present_limit (41,53), a3: cash_limit (42,54)
 * <p>
 * Example a1 (pos=80000): 1 2 42 2 84 12252276|999999999999|999999999999|80000|...
 * Example a2 (ecom=90000): 1 2 41 2 84 12253276|999999999999|999999999999|90000|...
 * Example a3 (cash=20000): 1 2 42 2 84 12254276|999999999999|999999999999|20000|...
 * <p>
 * All lengths calculated dynamically - no hardcoding.
 */
@Service
public class LimitCalculationService {

    private LimitMasterRepository limitMasterRepository;
    private LimitConfig limitConfig;

    public LimitCalculationService(LimitMasterRepository limitMasterRepository, LimitConfig limitConfig) {
        this.limitMasterRepository = limitMasterRepository;
        this.limitConfig = limitConfig;
    }

    public String buildLimitString(CardLimitDTO dto) {
        String posLimit = getValue(dto.getGoodsLimit());
        String ecomLimit = getValue(dto.getCardNotPresentLimit());
        String cashLimit = getValue(dto.getCashLimit());

        Map<String, LimitMaster> masterMap = getMasterMap();

        String f = limitConfig.getMax12();

        String a1 = buildPart(masterMap.get("goods_limit"), f, posLimit);
        String a2 = buildPart(masterMap.get("card_not_present_limit"), f, ecomLimit);
        String a3 = buildPart(masterMap.get("cash_limit"), f, cashLimit);

        return a1 + a2 + a3;
    }

    /**
     * Builds one part (a1, a2, or a3) with dynamic length encoding.
     * Structure: 1 2 {profile_nr} 2 {data_len} [inner_data]
     * Inner: 1 2 {rule_nr} 2 {limit_values_len} [limit_values]
     * limit_values_len is calculated from f|f|val|f|f|val|f (e.g. 76 for val="80000").
     */
    private String buildPart(LimitMaster master, String f, String value) {
        int profileNr = (master != null && master.getLimitPnr() != null) ? master.getLimitPnr() : limitConfig.getDefaultProfileNr();
        int ruleNr = (master != null && master.getLimitRuleNr() != null) ? master.getLimitRuleNr() : limitConfig.getDefaultRuleNr();

        String limitValues = buildLimitValues(f, value);
        int limitValuesLen = limitValues.length();

        String innerHeader = "1" + "2" + pad2(ruleNr) + "2" + pad2(limitValuesLen);
        String innerData = innerHeader + limitValues;
        int innerDataLen = innerData.length();

        String outerHeader = "1" + "2" + pad2(profileNr) + "2" + pad2(innerDataLen);
        return outerHeader + innerData;
    }

    private String buildLimitValues(String f, String value) {
        return f + "|" + f + "|" + value + "|" + f + "|" + f + "|" + value + "|" + f;
    }

    /**
     * Pads length to 2 digits (01-99). For 100+ uses 3 digits.
     */
    private String pad2(int n) {
        if (n < 0) n = 0;
        if (n <= 99) return String.format("%02d", n);
        return String.valueOf(n);
    }

    private String getValue(String val) {
        if (val == null || val.trim().isEmpty()) {
            return limitConfig.getDefaultValue();
        }
        return val.trim();
    }

    private Map<String, LimitMaster> getMasterMap() {
        List<LimitMaster> masters = limitMasterRepository.findAll();
        if (masters == null || masters.isEmpty()) {
            return Map.of();
        }
        return masters.stream()
                .filter(m -> m.getLimitName() != null)
                .collect(Collectors.toMap(LimitMaster::getLimitName, m -> m, (a, b) -> a));
    }
}
