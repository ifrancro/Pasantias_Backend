package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.ActivarSocioResponse;
import com.example.herbalife_clubes.dtos.auth.QrResponse;

/**
 * Servicio para manejar la activación de socios mediante QR.
 */
public interface SocioActivationService {
    
    /**
     * Activa un usuario básico como socio de un club.
     * 
     * @param clubId ID del club al que se asocia el socio
     * @param anfitrionId ID del anfitrión autenticado (debe ser dueño del club)
     * @param activationPayload Payload del QR escaneado (formato: "ACTIVATE:{userId}")
     * @param referidoPor Opcional: quién lo refirió
     * @param comoConocio Opcional: cómo conoció el club
     * @return Respuesta con datos de la membresía creada
     */
    ActivarSocioResponse activarSocio(Integer clubId, Integer anfitrionId, String activationPayload, 
                                      String referidoPor, String comoConocio);
    
    /**
     * Genera el número de socio único.
     * Formato: C{clubId}-{leftPad(membresiaId, 6)}
     * 
     * @param clubId ID del club
     * @param membresiaId ID de la membresía (ya guardada)
     * @return Número de socio único
     */
    String generarNumeroSocio(Integer clubId, Integer membresiaId);
    
    /**
     * Obtiene el QR de activación o definitivo según el estado del usuario.
     * 
     * @param usuarioId ID del usuario autenticado
     * @return Respuesta con el QR correspondiente
     */
    QrResponse obtenerQrUsuario(Integer usuarioId);
    
    /**
     * Obtiene el QR definitivo de un socio.
     * 
     * @param usuarioId ID del socio autenticado
     * @return Respuesta con el QR del socio
     */
    QrResponse obtenerQrSocio(Integer usuarioId);
}

