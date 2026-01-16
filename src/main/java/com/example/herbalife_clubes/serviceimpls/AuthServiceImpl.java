package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.auth.AuthenticationRequest;
import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.RegisterRequest;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        Usuario usuario = new Usuario();

        usuario.setNombre(request.getNombre() != null && !request.getNombre().isBlank() 
                ? request.getNombre() : "Usuario");
        usuario.setApellido(request.getApellido() != null && !request.getApellido().isBlank() 
                ? request.getApellido() : "Default");
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setTelefono(request.getTelefono());
        usuario.setRedesSociales(request.getRedesSociales());
        
        if (request.getFechaNacimiento() != null && !request.getFechaNacimiento().isBlank()) {
            try {
                usuario.setFechaNacimiento(LocalDate.parse(request.getFechaNacimiento(), 
                        DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                // Si hay error en el parseo, se deja null
            }
        }

        // Asignar rol - Siempre asignar USUARIO_BASICO por defecto
        // El rolId enviado por el cliente se IGNORA por seguridad (solo ADMIN puede asignar roles)
        Rol rolDefault = rolRepository.findByNombre("USUARIO_BASICO")
                .orElseGet(() -> {
                    Rol nuevoRol = new Rol();
                    nuevoRol.setNombre("USUARIO_BASICO");
                    return rolRepository.save(nuevoRol);
                });
        usuario.setRol(rolDefault);

        usuario.setEstado("ACTIVO");

        usuarioRepository.save(usuario);

        String jwtToken = jwtService.generateToken(usuario);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rolNombre(usuario.getRol().getNombre())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String jwtToken = jwtService.generateToken(usuario);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }
}

