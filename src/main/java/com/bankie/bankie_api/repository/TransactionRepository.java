package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(value = "SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.fromAccount fa " +
            "LEFT JOIN FETCH fa.user " +
            "LEFT JOIN FETCH t.toAccount ta " +
            "LEFT JOIN FETCH ta.user " +
            "WHERE t.fromIban IN :ibans OR t.toIban IN :ibans",
            countQuery = "SELECT count(t) FROM Transaction t WHERE t.fromIban IN :ibans OR t.toIban IN :ibans")
    Page<Transaction> findByIbanIn(@Param("ibans") Collection<String> ibans, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.timestamp >= :since AND (" +
            "(t.fromIban = :iban AND t.type IN (com.bankie.bankie_api.enums.TransactionType.TRANSFER, com.bankie.bankie_api.enums.TransactionType.WITHDRAWAL)) OR " +
            "(t.toIban = :iban AND t.type = com.bankie.bankie_api.enums.TransactionType.DEPOSIT))")
    BigDecimal sumDailyMovementsSince(@Param("iban") String iban,
                                      @Param("since") LocalDateTime since);

    @Query(value = "SELECT t FROM Transaction t " +
            "LEFT JOIN FETCH t.fromAccount fa " +
            "LEFT JOIN FETCH fa.user " +
            "LEFT JOIN FETCH t.toAccount ta " +
            "LEFT JOIN FETCH ta.user " +
            "WHERE (:id IS NULL OR t.initiatedBy = :id) " +
            "AND (:ownerIbans IS NULL OR t.fromIban IN :ownerIbans OR t.toIban IN :ownerIbans) " +
            "AND (:type IS NULL OR t.type = :type) " +
            "AND (:iban IS NULL OR LOWER(t.fromIban) LIKE LOWER(CONCAT('%', :iban, '%')) " +
            "OR LOWER(t.toIban) LIKE LOWER(CONCAT('%', :iban, '%'))) " +
            "AND (:start IS NULL OR t.timestamp >= :start) " +
            "AND (:end IS NULL OR t.timestamp <= :end) " +
            "AND (:min IS NULL OR t.amount >= :min) " +
            "AND (:max IS NULL OR t.amount <= :max) ",
            countQuery = "SELECT count(t) FROM Transaction t " +
                    "WHERE (:id IS NULL OR t.initiatedBy = :id) " +
                    "AND (:ownerIbans IS NULL OR t.fromIban IN :ownerIbans OR t.toIban IN :ownerIbans) " +
                    "AND (:type IS NULL OR t.type = :type) " +
                    "AND (:iban IS NULL OR LOWER(t.fromIban) LIKE LOWER(CONCAT('%', :iban, '%')) " +
                    "OR LOWER(t.toIban) LIKE LOWER(CONCAT('%', :iban, '%'))) " +
                    "AND (:start IS NULL OR t.timestamp >= :start) " +
                    "AND (:end IS NULL OR t.timestamp <= :end) " +
                    "AND (:min IS NULL OR t.amount >= :min) " +
                    "AND (:max IS NULL OR t.amount <= :max) ")
    Page<Transaction> findAllFiltered(
            @Param("id") Long id,
            @Param("ownerIbans") List<String> ownerIbans,
            @Param("type") TransactionType type,
            @Param("iban") String iban,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max,
            Pageable pageable);
}
