package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.entities.VerificationCodePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * Busca el código de verificación más reciente no usado para un usuario.
     */
    Optional<VerificationCode> findTopByUsuarioAndUsedFalseOrderByCreatedAtDesc(Usuario usuario);

    /**
     * Busca un código OTP válido por email, código y propósito.
     */
    @Query("SELECT vc FROM VerificationCode vc "
            + "WHERE vc.usuario.email = :email "
            + "AND vc.code = :code "
            + "AND vc.purpose = :purpose "
            + "AND vc.used = false "
            + "AND vc.expiresAt > :now")
    Optional<VerificationCode> findValidCode(
            @Param("email") String email,
            @Param("code") String code,
            @Param("purpose") VerificationCodePurpose purpose,
            @Param("now") LocalDateTime now);

    /**
     * OTP activo más reciente para incrementar intentos fallidos (reset password).
     */
    Optional<VerificationCode> findFirstByUsuario_EmailAndPurposeAndUsedFalseAndExpiresAtAfterAndFailedAttemptsLessThanOrderByCreatedAtDesc(
            String email,
            VerificationCodePurpose purpose,
            LocalDateTime now,
            int maxFailedAttempts);

    /**
     * Invalida códigos pendientes de un usuario para un propósito concreto.
     */
    @Modifying
    @Query("UPDATE VerificationCode vc SET vc.used = true "
            + "WHERE vc.usuario = :usuario AND vc.purpose = :purpose AND vc.used = false")
    void invalidateAllByUsuarioAndPurpose(
            @Param("usuario") Usuario usuario,
            @Param("purpose") VerificationCodePurpose purpose);

    /**
     * Cuenta cuántos códigos se han generado para un usuario/propósito en las últimas N horas.
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc "
            + "WHERE vc.usuario = :usuario "
            + "AND vc.purpose = :purpose "
            + "AND vc.createdAt > :since")
    long countRecentCodes(
            @Param("usuario") Usuario usuario,
            @Param("purpose") VerificationCodePurpose purpose,
            @Param("since") LocalDateTime since);

    Optional<VerificationCode> findTopByUsuarioAndPurposeOrderByCreatedAtDesc(
            Usuario usuario, VerificationCodePurpose purpose);

    /**
     * Borra los códigos de un conjunto de usuarios. La FK de verification_codes
     * no es ON DELETE CASCADE, así que hay que limpiarlos antes de borrar usuarios.
     */
    @Modifying
    @Query("DELETE FROM VerificationCode vc WHERE vc.usuario IN :usuarios")
    void deleteByUsuarioIn(@Param("usuarios") java.util.List<Usuario> usuarios);
}
