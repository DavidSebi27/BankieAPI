package com.bankie.bankie_api.entity;

import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    private String iban;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;
    @Builder.Default
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(precision = 19, scale = 2)
    private BigDecimal absoluteLimit;

    @Column(precision = 19, scale = 2)
    private BigDecimal dailyTransferLimit;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
