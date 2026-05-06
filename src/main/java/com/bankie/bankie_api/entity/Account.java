package com.bankie.bankie_api.entity;

import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

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

    private Double balance;
    @Builder.Default
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private Double absoluteLimit;
    private Double dailyTransferLimit;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
