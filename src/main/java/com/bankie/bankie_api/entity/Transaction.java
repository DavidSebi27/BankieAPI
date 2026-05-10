package com.bankie.bankie_api.entity;

import com.bankie.bankie_api.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String fromIban;
    private String toIban;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;
    @Builder.Default
    private String currency = "EUR";

    private LocalDateTime timestamp;

    private Long initiatedBy;
}
