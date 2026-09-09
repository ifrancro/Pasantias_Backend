package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.qr.QRValidacionRequest;
import com.example.herbalife_clubes.dtos.qr.QRValidacionResponse;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.serviceimpls.QRServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * La baja administrativa vive en usuarios.estado y no toca la membresia.
 * Sin esta validacion, un socio dado de baja no puede entrar a la app pero
 * sigue acumulando asistencias y beneficios presentando su QR en el club.
 */
@ExtendWith(MockitoExtension.class)
class QRSocioDesactivadoTest {

    private static final String NUMERO_SOCIO = "CT-00000100";

    @Mock
    private MembresiaRepository membresiaRepository;
    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private QRServiceImpl qrService;

    @Test
    void qrDeSocioConCuentaInactivaNoEsValido() {
        when(membresiaRepository.findByNumeroSocio(NUMERO_SOCIO))
                .thenReturn(Optional.of(membresia("ACTIVA", "INACTIVO")));

        QRValidacionResponse response = qrService.validarSocio(new QRValidacionRequest(NUMERO_SOCIO, null));

        assertFalse(response.getValido());
        assertTrue(response.getMensaje().contains("cuenta del socio"));
        assertEquals(NUMERO_SOCIO, response.getNumeroSocio());
    }

    @Test
    void qrDeSocioConCuentaActivaSigueSiendoValido() {
        when(membresiaRepository.findByNumeroSocio(NUMERO_SOCIO))
                .thenReturn(Optional.of(membresia("ACTIVA", "ACTIVO")));

        QRValidacionResponse response = qrService.validarSocio(new QRValidacionRequest(NUMERO_SOCIO, null));

        assertTrue(response.getValido());
        assertEquals(NUMERO_SOCIO, response.getNumeroSocio());
    }

    @Test
    void reactivarLaCuentaVuelveAValidarElMismoQrSinReemitirlo() {
        Membresia membresia = membresia("ACTIVA", "INACTIVO");
        when(membresiaRepository.findByNumeroSocio(NUMERO_SOCIO)).thenReturn(Optional.of(membresia));

        QRValidacionRequest mismoQr = new QRValidacionRequest(NUMERO_SOCIO, null);
        assertFalse(qrService.validarSocio(mismoQr).getValido());

        // El admin reactiva la cuenta: la membresia y el numero de socio no se tocan
        membresia.getUsuario().setEstado("ACTIVO");

        QRValidacionResponse response = qrService.validarSocio(mismoQr);
        assertTrue(response.getValido());
        assertEquals(NUMERO_SOCIO, response.getNumeroSocio());
        assertEquals("ACTIVA", membresia.getEstado());
    }

    @Test
    void estadoNullDelUsuarioNoRompeLaValidacion() {
        // Registros antiguos sin estado: isEnabled() los trata como activos
        when(membresiaRepository.findByNumeroSocio(NUMERO_SOCIO))
                .thenReturn(Optional.of(membresia("ACTIVA", null)));

        QRValidacionResponse response = qrService.validarSocio(new QRValidacionRequest(NUMERO_SOCIO, null));

        assertTrue(response.getValido());
    }

    @Test
    void membresiaNoActivaSigueTeniendoPrioridadEnElMensaje() {
        when(membresiaRepository.findByNumeroSocio(NUMERO_SOCIO))
                .thenReturn(Optional.of(membresia("SUSPENDIDA", "ACTIVO")));

        QRValidacionResponse response = qrService.validarSocio(new QRValidacionRequest(NUMERO_SOCIO, null));

        assertFalse(response.getValido());
        assertTrue(response.getMensaje().contains("SUSPENDIDA"));
    }

    private Membresia membresia(String estadoMembresia, String estadoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(42);
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setEstado(estadoUsuario);

        Membresia membresia = new Membresia();
        membresia.setId(100);
        membresia.setNumeroSocio(NUMERO_SOCIO);
        membresia.setEstado(estadoMembresia);
        membresia.setUsuario(usuario);
        return membresia;
    }
}
