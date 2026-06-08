package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.AtmRequestDTO;
import com.bankie.bankie_api.dto.request.TransferRequestDTO;
import com.bankie.bankie_api.dto.response.TransactionResponseDTO;
import com.bankie.bankie_api.enums.TransactionType;
import com.bankie.bankie_api.exception.BusinessRuleException;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.security.JwtService;
import com.bankie.bankie_api.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    private static final String FROM = "NL01BANK0000000001";
    private static final String TO = "NL01BANK0000000002";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TransactionService transactionService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private final Authentication customer = new UsernamePasswordAuthenticationToken(
            "alice@bankie.nl", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(customer);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TransactionResponseDTO txDto() {
        return TransactionResponseDTO.builder().id(1L).type(TransactionType.TRANSFER)
                .fromIban(FROM).toIban(TO).amount(new BigDecimal("50.00")).build();
    }

    @Test
    void getTransactions_returnsPage() throws Exception {
        when(transactionService.findAll(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(txDto())));

        mockMvc.perform(get("/transactions").principal(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void transfer_returns201() throws Exception {
        when(transactionService.transfer(any(), any())).thenReturn(txDto());

        mockMvc.perform(post("/transactions")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequestDTO(FROM, TO, new BigDecimal("50.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void transfer_invalidPayload_isBadRequest() throws Exception {
        // negative amount and malformed ibans
        String invalid = """
                {"fromIban":"bad","toIban":"also-bad","amount":-5}
                """;

        mockMvc.perform(post("/transactions")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_businessRuleViolation_returns422() throws Exception {
        when(transactionService.transfer(any(), any()))
                .thenThrow(new BusinessRuleException("Insufficient funds"));

        mockMvc.perform(post("/transactions")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequestDTO(FROM, TO, new BigDecimal("50.00")))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void withdraw_returns201() throws Exception {
        when(transactionService.withdraw(any(), any()))
                .thenReturn(TransactionResponseDTO.builder().id(2L).type(TransactionType.WITHDRAWAL).build());

        mockMvc.perform(post("/transactions/withdraw")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AtmRequestDTO(FROM, new BigDecimal("20.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
    }

    @Test
    void deposit_returns201() throws Exception {
        when(transactionService.deposit(any(), any()))
                .thenReturn(TransactionResponseDTO.builder().id(3L).type(TransactionType.DEPOSIT).build());

        mockMvc.perform(post("/transactions/deposit")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AtmRequestDTO(FROM, new BigDecimal("20.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    void deposit_invalidPayload_isBadRequest() throws Exception {
        mockMvc.perform(post("/transactions/deposit")
                        .principal(customer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iban\":\"bad\",\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }
}
