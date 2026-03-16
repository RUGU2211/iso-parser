package com.hitachi.iso_parser.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "limit_master")
public class LimitMaster {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String limitName;

	private Integer limitPnr;

	private Integer limitRuleNr;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLimitName() {
		return limitName;
	}

	public void setLimitName(String limitName) {
		this.limitName = limitName;
	}

	public Integer getLimitPnr() {
		return limitPnr;
	}

	public void setLimitPnr(Integer limitPnr) {
		this.limitPnr = limitPnr;
	}

	public Integer getLimitRuleNr() {
		return limitRuleNr;
	}

	public void setLimitRuleNr(Integer limitRuleNr) {
		this.limitRuleNr = limitRuleNr;
	}

}