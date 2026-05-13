package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.Transaction;
import com.bankie.bankie_api.enums.TransactionType;
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

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.fromIban = :iban AND t.type = :type AND t.timestamp >= :since")
    BigDecimal sumOutgoingSince(@Param("iban") String iban,
                                @Param("type") TransactionType type,
                                @Param("since") LocalDateTime since);
}
