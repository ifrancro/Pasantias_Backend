package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.qr.QRValidacionRequest;
import com.example.herbalife_clubes.dtos.qr.QRValidacionResponse;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.services.QRService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class QRServiceImpl implements QRService {
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private ClubRepository clubRepository;

    @Override
    public QRValidacionResponse validarSocio(QRValidacionRequest request) {
        QRValidacionResponse response = new QRValidacionResponse();
        
        if (request.getQr() == null || request.getQr().trim().isEmpty()) {
            response.setValido(false);
            response.setMensaje("QR no proporcionado o vacío");
            return response;
        }
        
        // Buscar membresía por número de socio o ID codificado en QR
        // El QR puede contener el número de socio o el ID de la membresía
        Membresia membresia = null;
        
        // Intentar buscar por número de socio
        Optional<Membresia> membresiaPorNumero = membresiaRepository.findByNumeroSocio(request.getQr());
        if (membresiaPorNumero.isPresent()) {
            membresia = membresiaPorNumero.get();
        } else {
            // Intentar buscar por ID (si el QR contiene el ID)
            try {
                Integer membresiaId = Integer.parseInt(request.getQr());
                Optional<Membresia> membresiaPorId = membresiaRepository.findById(membresiaId);
                if (membresiaPorId.isPresent()) {
                    membresia = membresiaPorId.get();
                }
            } catch (NumberFormatException e) {
                // El QR no es un número, continuar
            }
        }
        
        if (membresia == null) {
            response.setValido(false);
            response.setMensaje("Socio no encontrado con el QR proporcionado");
            return response;
        }
        
        // Validar que el socio esté activo
        if (membresia.getEstado() == null || !membresia.getEstado().equals("ACTIVA")) {
            response.setValido(false);
            response.setMensaje("El socio no está activo. Estado actual: " + membresia.getEstado());
            response.setMembresiaId(membresia.getId());
            response.setNumeroSocio(membresia.getNumeroSocio());
            return response;
        }
        
        // Validar club si se proporciona (opcional)
        if (request.getClubId() != null) {
            Club club = clubRepository.findById(request.getClubId())
                    .orElse(null);
            
            if (club == null) {
                response.setValido(false);
                response.setMensaje("Club no encontrado con id: " + request.getClubId());
                return response;
            }
            
            if (club.getEstado() == null || (!club.getEstado().equals("APROBADO") && !club.getEstado().equals("ACTIVO"))) {
                response.setValido(false);
                response.setMensaje("El club no está activo. Estado actual: " + club.getEstado());
                return response;
            }
        }
        
        // Socio válido y activo - puede gozar de beneficios en TODOS los clubes
        response.setValido(true);
        response.setMembresiaId(membresia.getId());
        response.setNumeroSocio(membresia.getNumeroSocio());
        
        if (membresia.getUsuario() != null) {
            String nombre = membresia.getUsuario().getNombre() != null ? membresia.getUsuario().getNombre() : "";
            String apellido = membresia.getUsuario().getApellido() != null ? membresia.getUsuario().getApellido() : "";
            response.setNombreCompleto((nombre + " " + apellido).trim());
        }
        
        response.setEstado(membresia.getEstado());
        
        if (membresia.getNivel() != null) {
            response.setNivelNombre(membresia.getNivel().getNombre());
        }
        
        response.setRachaActual(membresia.getRachaActual() != null ? membresia.getRachaActual() : 0);
        response.setRachaMaxima(membresia.getRachaMaxima() != null ? membresia.getRachaMaxima() : 0);
        response.setMensaje("Socio válido y activo. Puede gozar de beneficios en todos los clubes.");
        
        return response;
    }
}

