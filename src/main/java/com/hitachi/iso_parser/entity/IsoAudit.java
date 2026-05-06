package com.hitachi.iso_parser.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "iso_audit")
public class IsoAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "req_in", length = 8000)
    private String reqIn;

    @Column(name = "resp_out", length = 8000)
    private String respOut;

    @Column(name = "de39", length = 10)
    private String de39;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public IsoAudit() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReqIn() {
        return reqIn;
    }

    public void setReqIn(String reqIn) {
        this.reqIn = reqIn;
    }

    public String getRespOut() {
        return respOut;
    }

    public void setRespOut(String respOut) {
        this.respOut = respOut;
    }

    public String getDe39() {
        return de39;
    }

    public void setDe39(String de39) {
        this.de39 = de39;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
