package com.bankie.bankie_api.entity;

import com.bankie.bankie_api.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

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

    private Double amount;
    @Builder.Default
    private String currency = "EUR";

    private LocalDateTime timestamp;

    private Long initiatedBy;
}
