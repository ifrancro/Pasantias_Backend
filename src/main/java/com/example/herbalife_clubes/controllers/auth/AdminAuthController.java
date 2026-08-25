package com.example.herbalife_clubes.controllers.auth;

import com.example.herbalife_clubes.dtos.auth.AdminLoginResponse;
import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación exclusiva del panel administrador.
 * Público: solo POST /login. El resto de /api/admin/** permanece protegido.
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.authenticateAdmin(request));
    }
}
