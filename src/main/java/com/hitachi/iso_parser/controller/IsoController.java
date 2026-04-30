package com.hitachi.iso_parser.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.service.IsoMessageService;

@RestController
@RequestMapping("/api/iso/limit")
public class IsoController {

    private IsoMessageService isoMessageService;
    private IsoConfig isoConfig;

    public IsoController(IsoMessageService isoMessageService, IsoConfig isoConfig) {
        this.isoMessageService = isoMessageService;
        this.isoConfig = isoConfig;
    }

    @PostMapping(value = "/{pan}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<IsoParseResponse> parseIsoRaw(@PathVariable("pan") String pan, @RequestBody(required = false) String hexMessage) {
        String message = (hexMessage != null) ? hexMessage.trim() : "";
        return processAndRespond(message);
    }

    private ResponseEntity<IsoParseResponse> processAndRespond(String message) {
        IsoParseResponse response = isoMessageService.processIsoMessage(message);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(response.getDe39() != null && isoConfig.getDe39Failed().equals(response.getDe39()) ? 400 : 500).body(response);
    }
}
