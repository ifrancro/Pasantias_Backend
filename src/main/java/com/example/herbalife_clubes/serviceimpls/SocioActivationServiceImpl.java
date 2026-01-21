package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.auth.ActivarSocioResponse;
import com.example.herbalife_clubes.dtos.auth.QrResponse;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ConflictException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.SocioActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación del servicio de activación de socios.
 * Maneja toda la lógica de negocio para activar usuarios básicos como socios.
 */
@Service
@RequiredArgsConstructor
public class SocioActivationServiceImpl implements SocioActivationService {

    private final UsuarioRepository usuarioRepository;
    private final ClubRepository clubRepository;
    private final MembresiaRepository membresiaRepository;
    private final RolRepository rolRepository;

    /**
     * FLUJO DE ACTIVACIÓN:
     * 1. Validar que el anfitrión es dueño del club (403 si no)
     * 2. Parsear userId desde activationPayload (400 si formato inválido)
     * 3. Verificar que el usuario existe (404 si no)
     * 4. Verificar que el usuario es USUARIO_BASICO (409 si ya es SOCIO)
     * 5. Verificar que NO existe membresía previa (409 si existe)
     * 6. Crear membresía con datos recibidos
     * 7. Generar numero_socio único
     * 8. Actualizar rol del usuario a SOCIO
     * 9. Devolver respuesta con datos
     */
    @Override
    @Transactional
    public ActivarSocioResponse activarSocio(Integer clubId, Integer anfitrionId, String activationPayload,
                                             String referidoPor, String comoConocio) {
        
        // 1. Validar que el anfitrión es dueño del club
        Club club = clubRepository.findByIdAndAnfitrionId(clubId, anfitrionId)
                .orElseThrow(() -> new AccessDeniedException("No eres el anfitrión de este club"));

        // 2. Parsear userId desde activationPayload
        Integer usuarioId = parsearUsuarioIdDesdePayload(activationPayload);
        
        // 3. Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // 4. Verificar que el usuario es USUARIO_BASICO
        String rolActual = usuario.getRol().getNombre();
        if (!"USUARIO_BASICO".equalsIgnoreCase(rolActual)) {
            throw new ConflictException("El usuario ya es socio o tiene otro rol. No se puede activar nuevamente.");
        }

        // 5. Verificar que NO existe membresía previa
        if (membresiaRepository.existsByUsuarioId(usuarioId)) {
            throw new ConflictException("El usuario ya tiene una membresía activa");
        }

        // 6. Crear membresía
        Membresia membresia = new Membresia();
        membresia.setUsuario(usuario);
        membresia.setClub(club);
        membresia.setReferidoPor(referidoPor);
        membresia.setComoConocio(comoConocio);
        membresia.setEstado("ACTIVA");
        // puntosAcumulados y fechaRegistro se establecen en @PrePersist

        // Guardar membresía para obtener el ID
        membresia = membresiaRepository.save(membresia);

        // 7. Generar numero_socio único
        String numeroSocio = generarNumeroSocio(clubId, membresia.getId());
        membresia.setNumeroSocio(numeroSocio);
        membresia = membresiaRepository.save(membresia);

        // 8. Actualizar rol del usuario a SOCIO
        Rol rolSocio = rolRepository.findByNombre("SOCIO")
                .orElseThrow(() -> new ResourceNotFoundException("Rol SOCIO no encontrado en la base de datos"));
        usuario.setRol(rolSocio);
        usuarioRepository.save(usuario);

        // 9. Construir y devolver respuesta
        return ActivarSocioResponse.builder()
                .membresiaId(membresia.getId())
                .numeroSocio(numeroSocio)
                .clubId(clubId)
                .clubNombre(club.getNombreClub())
                .usuarioId(usuarioId)
                .usuarioNombre(usuario.getNombre())
                .usuarioApellido(usuario.getApellido())
                .qrSocioPayload("SOCIO:" + numeroSocio)
                .build();
    }

    /**
     * Parsea el userId desde el payload del QR.
     * Formato esperado: "ACTIVATE:{userId}"
     * 
     * @param activationPayload Payload del QR
     * @return ID del usuario
     * @throws IllegalArgumentException si el formato es inválido
     */
    private Integer parsearUsuarioIdDesdePayload(String activationPayload) {
        if (activationPayload == null || activationPayload.isBlank()) {
            throw new IllegalArgumentException("El payload de activación no puede estar vacío");
        }

        // Si ya viene solo el número, intentar parsearlo directamente
        try {
            return Integer.parseInt(activationPayload.trim());
        } catch (NumberFormatException e) {
            // Continuar con el parseo del formato "ACTIVATE:{id}"
        }

        // Parsear formato "ACTIVATE:{userId}"
        if (activationPayload.startsWith("ACTIVATE:")) {
            String userIdStr = activationPayload.substring("ACTIVATE:".length()).trim();
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Formato de payload inválido. Se espera 'ACTIVATE:{userId}' o solo el número");
            }
        }

        throw new IllegalArgumentException("Formato de payload inválido. Se espera 'ACTIVATE:{userId}' o solo el número");
    }

    @Override
    public String generarNumeroSocio(Integer clubId, Integer membresiaId) {
        // Formato: C{clubId}-{leftPad(membresiaId, 6)}
        // Ejemplo: C12-000123
        String paddedId = String.format("%06d", membresiaId);
        String numeroSocio = "C" + clubId + "-" + paddedId;
        
        // Verificar unicidad (por si acaso, aunque es muy improbable que haya colisión)
        Optional<Membresia> existente = membresiaRepository.findByNumeroSocio(numeroSocio);
        int intentos = 0;
        while (existente.isPresent() && intentos < 10) {
            // Si hay colisión (muy improbable), agregar un sufijo
            numeroSocio = "C" + clubId + "-" + paddedId + "-" + intentos;
            existente = membresiaRepository.findByNumeroSocio(numeroSocio);
            intentos++;
        }
        
        if (intentos >= 10) {
            throw new RuntimeException("No se pudo generar un número de socio único después de múltiples intentos");
        }
        
        return numeroSocio;
    }

    @Override
    public QrResponse obtenerQrUsuario(Integer usuarioId) {
        // Verificar si ya tiene membresía
        Optional<Membresia> membresiaOpt = membresiaRepository.findByUsuarioId(usuarioId);

        if (membresiaOpt.isPresent()) {
            // Si ya tiene membresía, devolver QR definitivo de socio
            Membresia membresia = membresiaOpt.get();
            Club club = membresia.getClub();
            
            // Extraer solo los números del numeroSocio para el campo numeroSocio (opcional)
            String numeroSocioStr = membresia.getNumeroSocio();
            Integer numeroSocioInt = null;
            try {
                // Intentar extraer solo los dígitos del formato "C12-000123"
                String soloNumeros = numeroSocioStr.replaceAll("\\D", "");
                if (!soloNumeros.isEmpty()) {
                    numeroSocioInt = Integer.parseInt(soloNumeros);
                }
            } catch (NumberFormatException e) {
                // Si falla, dejamos null
            }
            
            return QrResponse.builder()
                    .tipo("SOCIO")
                    .qrPayload("SOCIO:" + numeroSocioStr)
                    .numeroSocio(numeroSocioInt)
                    .clubId(club.getId())
                    .clubNombre(club.getNombreClub())
                    .hubId(club.getHub() != null ? club.getHub().getId() : null)
                    .build();
        } else {
            // Si no tiene membresía, devolver QR de activación
            return QrResponse.builder()
                    .tipo("ACTIVACION")
                    .qrPayload("ACTIVATE:" + usuarioId)
                    .build();
        }
    }

    @Override
    public QrResponse obtenerQrSocio(Integer usuarioId) {
        Membresia membresia = membresiaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró membresía para este usuario"));

        Club club = membresia.getClub();
        
        // Extraer solo los números del numeroSocio para el campo numeroSocio (opcional)
        String numeroSocioStr = membresia.getNumeroSocio();
        Integer numeroSocioInt = null;
        try {
            // Intentar extraer solo los dígitos del formato "C12-000123"
            String soloNumeros = numeroSocioStr.replaceAll("\\D", "");
            if (!soloNumeros.isEmpty()) {
                numeroSocioInt = Integer.parseInt(soloNumeros);
            }
        } catch (NumberFormatException e) {
            // Si falla, dejamos null
        }
        
        return QrResponse.builder()
                .tipo("SOCIO")
                .qrPayload("SOCIO:" + numeroSocioStr)
                .numeroSocio(numeroSocioInt)
                .clubId(club.getId())
                .clubNombre(club.getNombreClub())
                .hubId(club.getHub() != null ? club.getHub().getId() : null)
                .build();
    }
}

