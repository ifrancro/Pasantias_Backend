package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.AdminLoginResponse;
import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.RegisterRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoRequest;
import com.example.herbalife_clubes.dtos.auth.RegisterBasicoResponse;

public interface AuthService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
    /**
     * Login exclusivo del panel admin: valida credenciales y solo emite JWT si el rol es ADMIN.
     */
    AdminLoginResponse authenticateAdmin(AuthenticationRequest request);
    RegisterBasicoResponse registerBasico(RegisterBasicoRequest request);
}

