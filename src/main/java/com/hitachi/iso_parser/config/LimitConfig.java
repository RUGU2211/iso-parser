package com.hitachi.iso_parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "limit")
public class LimitConfig {

    private String max21 = "999999999999999999999999";
    private String max12 = "999999999999";
    private String prefix1 = "1242292125228510";
    private String prefix2 = "1241292125328510";
    private String prefix3 = "1242292125228510";
    private String defaultValue = "0";
    private int defaultProfileNr = 42;
    private int defaultRuleNr = 52;

    public String getMax21() {
        return max21;
    }

    public void setMax21(String max21) {
        this.max21 = max21;
    }

    public String getMax12() {
        return max12;
    }

    public void setMax12(String max12) {
        this.max12 = max12;
    }

    public String getPrefix1() {
        return prefix1;
    }

    public void setPrefix1(String prefix1) {
        this.prefix1 = prefix1;
    }

    public String getPrefix2() {
        return prefix2;
    }

    public void setPrefix2(String prefix2) {
        this.prefix2 = prefix2;
    }

    public String getPrefix3() {
        return prefix3;
    }

    public void setPrefix3(String prefix3) {
        this.prefix3 = prefix3;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getDefaultProfileNr() {
        return defaultProfileNr;
    }

    public void setDefaultProfileNr(int defaultProfileNr) {
        this.defaultProfileNr = defaultProfileNr;
    }

    public int getDefaultRuleNr() {
        return defaultRuleNr;
    }

    public void setDefaultRuleNr(int defaultRuleNr) {
        this.defaultRuleNr = defaultRuleNr;
    }
}
