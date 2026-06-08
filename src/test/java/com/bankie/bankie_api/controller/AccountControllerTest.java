package com.bankie.bankie_api.controller;

import com.bankie.bankie_api.dto.request.CreateAccountRequestDTO;
import com.bankie.bankie_api.dto.request.SetAbsoluteLimitRequestDTO;
import com.bankie.bankie_api.dto.request.SetDailyLimitRequestDTO;
import com.bankie.bankie_api.dto.response.AccountResponseDTO;
import com.bankie.bankie_api.dto.response.SearchAccountResponseDTO;
import com.bankie.bankie_api.enums.AccountStatus;
import com.bankie.bankie_api.enums.AccountType;
import com.bankie.bankie_api.exception.AccountNotFoundException;
import com.bankie.bankie_api.repository.UserRepository;
import com.bankie.bankie_api.security.JwtService;
import com.bankie.bankie_api.service.AccountService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final String IBAN = "NL01INHO0000000001";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AccountService accountService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private final Authentication employee = new UsernamePasswordAuthenticationToken(
            "emp@bankie.nl", null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(employee);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AccountResponseDTO accountDto() {
        return AccountResponseDTO.builder()
                .iban(IBAN).type(AccountType.CHECKING).balance(new BigDecimal("100.00"))
                .currency("EUR").status(AccountStatus.ACTIVE).userId(1L).build();
    }

    @Test
    void getAccounts_returnsPage() throws Exception {
        when(accountService.getAccountsForUser(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(accountDto())));

        mockMvc.perform(get("/accounts").principal(employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iban").value(IBAN));
    }

    @Test
    void searchAccounts_returnsResults() throws Exception {
        when(accountService.searchAccounts(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        SearchAccountResponseDTO.builder().firstName("John").lastName("Doe").iban(IBAN).build())));

        mockMvc.perform(get("/accounts/search")
                        .param("firstName", "John").param("lastName", "Doe")
                        .principal(employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iban").value(IBAN));
    }

    @Test
    void searchAccounts_blankNames_isBadRequest() throws Exception {
        mockMvc.perform(get("/accounts/search")
                        .param("firstName", "").param("lastName", "")
                        .principal(employee))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyRecipient_returnsMatch() throws Exception {
        when(accountService.verifyRecipient(any()))
                .thenReturn(SearchAccountResponseDTO.builder().firstName("John").lastName("Doe").iban(IBAN).build());

        mockMvc.perform(get("/accounts/verify-recipient")
                        .param("iban", IBAN).param("firstName", "John").param("lastName", "Doe")
                        .principal(employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(IBAN));
    }

    @Test
    void verifyRecipient_notFound_returns404() throws Exception {
        when(accountService.verifyRecipient(any())).thenThrow(new AccountNotFoundException(IBAN));

        mockMvc.perform(get("/accounts/verify-recipient")
                        .param("iban", IBAN).param("firstName", "John").param("lastName", "Doe")
                        .principal(employee))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveCustomer_returns201WithAccounts() throws Exception {
        when(accountService.approveCustomerAndCreateAccounts(eq(5L), any()))
                .thenReturn(List.of(accountDto()));

        mockMvc.perform(post("/accounts/customers/5/approve")
                        .principal(employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequestDTO(null, new BigDecimal("500.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].iban").value(IBAN));
    }

    @Test
    void closeAccount_returnsOk() throws Exception {
        when(accountService.closeAccount(IBAN)).thenReturn(accountDto());

        mockMvc.perform(patch("/accounts/{iban}/close", IBAN).principal(employee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iban").value(IBAN));
    }

    @Test
    void updateAbsoluteLimit_returnsOk() throws Exception {
        when(accountService.updateAbsoluteLimit(eq(IBAN), any())).thenReturn(accountDto());
        var dto = new SetAbsoluteLimitRequestDTO();
        dto.setAbsoluteLimit(new BigDecimal("-100.00"));

        mockMvc.perform(patch("/accounts/{iban}/absolute-limit", IBAN)
                        .principal(employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateDailyLimit_returnsOk() throws Exception {
        when(accountService.updateDailyTransferLimit(eq(IBAN), any())).thenReturn(accountDto());
        var dto = new SetDailyLimitRequestDTO();
        dto.setDailyTransferLimit(new BigDecimal("500.00"));

        mockMvc.perform(patch("/accounts/{iban}/daily-limit", IBAN)
                        .principal(employee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
