package com.hitachi.iso_parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.config.LimitConfig;

@SpringBootApplication
@EnableConfigurationProperties({IsoConfig.class, LimitConfig.class})
public class IsoParserApplication {
	public static void main(String[] args) {
		SpringApplication.run(IsoParserApplication.class, args);
	}
}
