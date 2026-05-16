package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.fromIban IN :ibans OR t.toIban IN :ibans")
    Page<Transaction> findByIbanIn(@Param("ibans") Collection<String> ibans, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.timestamp >= :since AND (" +
            "(t.fromIban = :iban AND t.type IN (com.bankie.bankie_api.enums.TransactionType.TRANSFER, com.bankie.bankie_api.enums.TransactionType.WITHDRAWAL)) OR " +
            "(t.toIban = :iban AND t.type = com.bankie.bankie_api.enums.TransactionType.DEPOSIT))")
    BigDecimal sumDailyMovementsSince(@Param("iban") String iban,
                                      @Param("since") LocalDateTime since);
}
