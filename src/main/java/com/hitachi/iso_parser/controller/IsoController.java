package com.hitachi.iso_parser.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hitachi.iso_parser.config.IsoConfig;
import com.hitachi.iso_parser.dto.IsoParseRequest;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.service.IsoMessageService;

@RestController
@RequestMapping("/api/iso")
public class IsoController {

    private IsoMessageService isoMessageService;
    private IsoConfig isoConfig;

    public IsoController(IsoMessageService isoMessageService, IsoConfig isoConfig) {
        this.isoMessageService = isoMessageService;
        this.isoConfig = isoConfig;
    }

    @PostMapping(value = "/parse", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<IsoParseResponse> parseIsoRaw(@RequestBody(required = false) String hexMessage) {
        String message = (hexMessage != null) ? hexMessage.trim() : "";
        return processAndRespond(message);
    }

    @PostMapping(value = "/parse", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IsoParseResponse> parseIsoJson(@RequestBody(required = false) IsoParseRequest request) {
        String message = "";
        if (request != null && request.getIsoMessage() != null) {
            message = request.getIsoMessage().trim();
        }
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
