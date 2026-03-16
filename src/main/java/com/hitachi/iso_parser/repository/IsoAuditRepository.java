package com.hitachi.iso_parser.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.iso_parser.entity.IsoAudit;

public interface IsoAuditRepository extends JpaRepository<IsoAudit, Long> {
}
