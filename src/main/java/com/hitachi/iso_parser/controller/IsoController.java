package com.hitachi.iso_parser.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hitachi.iso_parser.dto.IsoParseRequest;
import com.hitachi.iso_parser.dto.IsoParseResponse;
import com.hitachi.iso_parser.exception.IsoParseException;
import com.hitachi.iso_parser.exception.XmlParseException;
import com.hitachi.iso_parser.service.IsoMessageService;

@RestController
@RequestMapping("/api/iso")
public class IsoController {

    private IsoMessageService isoMessageService;

    public IsoController(IsoMessageService isoMessageService) {
        this.isoMessageService = isoMessageService;
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
        try {
            isoMessageService.processIsoMessage(message);
            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(true);
            response.setMessage("ISO message processed successfully");
            return ResponseEntity.ok(response);
        } catch (IsoParseException e) {
            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(false);
            response.setMessage("ISO parse failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (XmlParseException e) {
            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(false);
            response.setMessage("XML parse failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            IsoParseResponse response = new IsoParseResponse();
            response.setSuccess(false);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
