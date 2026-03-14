package com.hitachi.iso_parser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "limit")
@Data
public class LimitConfig {

    private String max21 = "999999999999999999999999";
    private String max12 = "999999999999";
    private String prefix1 = "124229212522810";
    private String prefix2 = "1241292125328510";
    private String defaultValue = "0";
}
