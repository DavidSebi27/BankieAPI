package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDTO>> getAllTransactions(
            @RequestParam(required = false) Long initiatedBy,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @ParameterObject @PageableDefault(size = 5, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<TransactionResponseDTO> response = transactionService.findAll(initiatedBy, type, iban, start, end, minAmount, maxAmount, pageable, authentication);

        return ResponseEntity.ok(response);
    }
}
