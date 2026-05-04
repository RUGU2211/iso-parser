package com.hitachi.iso_parser.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hitachi.iso_parser.entity.CardLimit;

@Repository
public interface CardLimitRepository extends JpaRepository<CardLimit, Long> {

    Optional<CardLimit> findByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(Integer issuerNr, String pan, String seqNr);

    Optional<CardLimit> findFirstByIssuerNrAndPanAndSeqNrAndDateDeletedIsNotNullOrderByDateDeletedDesc(
            Integer issuerNr, String pan, String seqNr);

    boolean existsByIssuerNrAndPanAndSeqNrAndDateDeletedIsNull(Integer issuerNr, String pan, String seqNr);

    List<CardLimit> findAllByPanAndDateDeletedIsNullOrderBySeqNrAsc(String pan);

    Optional<CardLimit> findFirstByPanAndDateDeletedIsNotNullOrderByDateDeletedDesc(String pan);

    /**
     * Soft delete: sets {@code date_deleted} for all active rows with this PAN. Rows remain in the table.
     *
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CardLimit c SET c.dateDeleted = :deletedAt WHERE c.pan = :pan AND c.dateDeleted IS NULL")
    int softDeleteByPan(@Param("pan") String pan, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CardLimit c SET c.dateDeleted = :deletedAt, c.lastUpdatedDate = :deletedAt, c.lastUpdatedUser = :deletedByUser WHERE c.pan = :pan AND c.dateDeleted IS NULL")
    int softDeleteByPanWithUser(@Param("pan") String pan, @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedByUser") String deletedByUser);
}
