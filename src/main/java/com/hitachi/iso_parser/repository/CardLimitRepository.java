package com.hitachi.iso_parser.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.iso_parser.entity.CardLimit;

@Repository
public interface CardLimitRepository extends JpaRepository<CardLimit,Long> {

 Optional<CardLimit> findByPan(String pan);

 Optional<CardLimit> findByPanAndSeqNr(String pan, String seqNr);

}
