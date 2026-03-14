package com.hitachi.iso_parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "iso")
@Data
public class IsoConfig {

    private int defaultNr = 13;
    private String defaultSeqNr = "001";
    private String lastUpdUser = "sp";
}
