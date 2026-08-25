package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.AdminLoginResponse;
import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.exceptions.AdminAccessDeniedException;
import com.example.herbalife_clubes.services.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminAuthController controller;

    @Test
    void adminValidoDevuelve200ConToken() {
        when(authService.authenticateAdmin(any())).thenReturn(AdminLoginResponse.builder()
                .token("jwt-admin")
                .userId(1)
                .email("admin@demo.com")
                .nombre("Admin")
                .apellido("Corporativo")
                .rolNombre("ADMIN")
                .build());

        ResponseEntity<AdminLoginResponse> response = controller.login(
                AuthenticationRequest.builder()
                        .email("admin@demo.com")
                        .password("secret")
                        .build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-admin", response.getBody().getToken());
        assertEquals("ADMIN", response.getBody().getRolNombre());
    }

    @Test
    void noAdminPropagaAdminAccessDenied() {
        when(authService.authenticateAdmin(any()))
                .thenThrow(new AdminAccessDeniedException());

        assertThrows(
                AdminAccessDeniedException.class,
                () -> controller.login(AuthenticationRequest.builder()
                        .email("socio@demo.com")
                        .password("secret")
                        .build()));
    }

    @Test
    void passwordIncorrectaPropagaBadCredentials() {
        when(authService.authenticateAdmin(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(
                BadCredentialsException.class,
                () -> controller.login(AuthenticationRequest.builder()
                        .email("admin@demo.com")
                        .password("wrong")
                        .build()));
    }

    @Test
    void adminDeshabilitadoPropagaDisabled() {
        when(authService.authenticateAdmin(any()))
                .thenThrow(new DisabledException("disabled"));

        assertThrows(
                DisabledException.class,
                () -> controller.login(AuthenticationRequest.builder()
                        .email("admin@demo.com")
                        .password("secret")
                        .build()));
    }
}
