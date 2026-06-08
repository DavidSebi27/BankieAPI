package com.bankie.bankie_api.security;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import com.bankie.bankie_api.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @Mock UserRepository userRepository;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User approvedUser() {
        return User.builder().id(7L).email("alice@bankie.nl").role(Role.CUSTOMER).approved(true).build();
    }

    @Test
    void noAuthorizationHeader_leavesContextEmptyAndProceeds() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void validTokenForApprovedUser_populatesAuthentication() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("7");
        when(jwtService.parse("good-token")).thenReturn(claims);
        when(userRepository.findById(7L)).thenReturn(Optional.of(approvedUser()));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice@bankie.nl");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
        assertThat(chain.getResponse()).isSameAs(response);
    }

    @Test
    void unapprovedUser_doesNotAuthenticate() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("7");
        when(jwtService.parse("good-token")).thenReturn(claims);
        User unapproved = approvedUser();
        unapproved.setApproved(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(unapproved));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidToken_clearsContextAndStillProceeds() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        when(jwtService.parse("bad-token")).thenThrow(new JwtException("invalid"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void nonNumericSubject_clearsContextAndProceeds() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("not-a-number");
        when(jwtService.parse("good-token")).thenReturn(claims);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(((HttpServletResponse) chain.getResponse()).getStatus()).isEqualTo(200);
    }
}
