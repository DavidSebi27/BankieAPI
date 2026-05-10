package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public Page<TransactionResponseDTO> list(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.findAll(pageable);
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> transfer(
            @RequestBody TransferRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.performTransfer(dto, authentication));
    }
}