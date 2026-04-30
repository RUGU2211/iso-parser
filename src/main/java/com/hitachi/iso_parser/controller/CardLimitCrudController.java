package com.hitachi.iso_parser.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hitachi.iso_parser.dto.LimitRecordResponse;
import com.hitachi.iso_parser.dto.LimitUpsertRequest;
import com.hitachi.iso_parser.service.CardLimitCrudService;

@RestController
@RequestMapping("/api/iso/limit")
public class CardLimitCrudController {

    private CardLimitCrudService cardLimitCrudService;

    public CardLimitCrudController(CardLimitCrudService cardLimitCrudService) {
        this.cardLimitCrudService = cardLimitCrudService;
    }

    @GetMapping("/{pan}")
    public ResponseEntity<List<LimitRecordResponse>> getByPan(@PathVariable("pan") String pan) {
        return ResponseEntity.ok(cardLimitCrudService.getByPan(pan));
    }

    @PostMapping(value = "/{pan}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LimitRecordResponse> create(@PathVariable("pan") String pan, @RequestBody LimitUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardLimitCrudService.create(pan, request));
    }

    @PutMapping(value = "/{pan}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LimitRecordResponse> upsert(@PathVariable("pan") String pan, @RequestBody LimitUpsertRequest request) {
        return ResponseEntity.ok(cardLimitCrudService.upsert(pan, request));
    }

    @DeleteMapping("/{pan}")
    public ResponseEntity<Void> delete(@PathVariable("pan") String pan) {
        cardLimitCrudService.delete(pan);
        return ResponseEntity.noContent().build();
    }
}
