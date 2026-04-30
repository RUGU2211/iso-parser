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

	@Column(length = 30)
	private String limitType;

	private Integer priority;

	private Boolean isActive = Boolean.TRUE;

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

	public String getLimitType() {
		return limitType;
	}

	public void setLimitType(String limitType) {
		this.limitType = limitType;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

}