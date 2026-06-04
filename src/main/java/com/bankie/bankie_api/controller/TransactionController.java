package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.AuthContext;
import com.bankie.bankie_api.dto.request.AtmRequestDTO;
import com.bankie.bankie_api.dto.request.TransactionFilterDTO;
import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDTO>> getAllTransactions(
            @ParameterObject TransactionFilterDTO filter,
            @ParameterObject @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(transactionService.findAll(filter, pageable, AuthContext.from(authentication)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDTO transfer(@Valid @RequestBody TransferRequestDTO request,
                                           Authentication authentication) {
        return transactionService.transfer(request, AuthContext.from(authentication));
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDTO withdraw(@Valid @RequestBody AtmRequestDTO request,
                                           Authentication authentication) {
        return transactionService.withdraw(request, AuthContext.from(authentication));
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDTO deposit(@Valid @RequestBody AtmRequestDTO request,
                                          Authentication authentication) {
        return transactionService.deposit(request, AuthContext.from(authentication));
    }
}
