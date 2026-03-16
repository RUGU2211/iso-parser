package com.hitachi.iso_parser.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "card_limits", uniqueConstraints = @UniqueConstraint(columnNames = {"pan", "seq_nr"}))
public class CardLimit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Integer isoNr;

	private String pan;

	private String seqNr;

	@Column(length = 1000)
	private String limits;

	private LocalDateTime lastUpdDate;

	private String lastUpdUser;

	@Column(name = "date_date")
	private LocalDate dateDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getIsoNr() {
		return isoNr;
	}

	public void setIsoNr(Integer isoNr) {
		this.isoNr = isoNr;
	}

	public String getPan() {
		return pan;
	}

	public void setPan(String pan) {
		this.pan = pan;
	}

	public String getSeqNr() {
		return seqNr;
	}

	public void setSeqNr(String seqNr) {
		this.seqNr = seqNr;
	}

	public String getLimits() {
		return limits;
	}

	public void setLimits(String limits) {
		this.limits = limits;
	}

	public LocalDateTime getLastUpdDate() {
		return lastUpdDate;
	}

	public void setLastUpdDate(LocalDateTime lastUpdDate) {
		this.lastUpdDate = lastUpdDate;
	}

	public String getLastUpdUser() {
		return lastUpdUser;
	}

	public void setLastUpdUser(String lastUpdUser) {
		this.lastUpdUser = lastUpdUser;
	}

	public LocalDate getDateDate() {
		return dateDate;
	}

	public void setDateDate(LocalDate dateDate) {
		this.dateDate = dateDate;
	}

}