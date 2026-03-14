package com.hitachi.iso_parser.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.iso_parser.entity.LimitMaster;

@Repository
public interface LimitMasterRepository extends JpaRepository<LimitMaster,Long> {

 Optional<LimitMaster> findByLimitName(String limitName);

}
