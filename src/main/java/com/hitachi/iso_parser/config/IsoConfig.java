package com.hitachi.iso_parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "iso")
public class IsoConfig {

    private int defaultNr = 13;
    private String defaultSeqNr = "001";
    private String lastUpdUser = "sp";
    private String de39Success = "00";
    private String de39Failed = "01";

    public int getDefaultNr() {
        return defaultNr;
    }

    public void setDefaultNr(int defaultNr) {
        this.defaultNr = defaultNr;
    }

    public String getDefaultSeqNr() {
        return defaultSeqNr;
    }

    public void setDefaultSeqNr(String defaultSeqNr) {
        this.defaultSeqNr = defaultSeqNr;
    }

    public String getLastUpdUser() {
        return lastUpdUser;
    }

    public void setLastUpdUser(String lastUpdUser) {
        this.lastUpdUser = lastUpdUser;
    }

    public String getDe39Success() {
        return de39Success;
    }

    public void setDe39Success(String de39Success) {
        this.de39Success = de39Success;
    }

    public String getDe39Failed() {
        return de39Failed;
    }

    public void setDe39Failed(String de39Failed) {
        this.de39Failed = de39Failed;
    }
}
