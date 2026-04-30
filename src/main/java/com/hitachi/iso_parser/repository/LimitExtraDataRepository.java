package com.hitachi.iso_parser.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.iso_parser.entity.LimitExtraData;

public interface LimitExtraDataRepository extends JpaRepository<LimitExtraData, Long> {

    List<LimitExtraData> findByPanAndSeqNr(String pan, String seqNr);

    List<LimitExtraData> findByPan(String pan);

    void deleteByPanAndSeqNr(String pan, String seqNr);

    void deleteByPan(String pan);
}
