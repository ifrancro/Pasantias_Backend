package com.example.herbalife_clubes.scheduled;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.repositories.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Borra los registros que nunca verificaron su correo.
 *
 * Sin esto, un registro abandonado deja el email ocupado para siempre (usuarios.email
 * es UNIQUE) y la persona no puede volver a intentarlo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingRegistrationCleanup {

    private final UsuarioRepository usuarioRepository;
    private final VerificationCodeRepository verificationCodeRepository;

    @Value("${app.verification.pending-ttl-hours:24}")
    private int pendingTtlHours;

    /** Todos los días a las 04:00. */
    @Scheduled(cron = "${app.verification.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void purgarRegistrosPendientes() {
        LocalDateTime limite = LocalDateTime.now().minusHours(pendingTtlHours);
        List<Usuario> vencidos = usuarioRepository.findPendientesVencidos(limite);

        if (vencidos.isEmpty()) {
            log.debug("Purga de registros pendientes: nada que borrar");
            return;
        }

        verificationCodeRepository.deleteByUsuarioIn(vencidos);
        usuarioRepository.deleteAll(vencidos);

        log.info("Purga de registros pendientes: {} usuarios eliminados (más de {}h sin verificar)",
                vencidos.size(), pendingTtlHours);
    }
}
