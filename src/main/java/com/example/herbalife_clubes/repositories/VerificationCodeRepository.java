package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.VerificationCode;
import com.example.herbalife_clubes.entities.Usuario;
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
     * Busca un código de verificación válido por email y código.
     */
    @Query("SELECT vc FROM VerificationCode vc " +
           "WHERE vc.usuario.email = :email " +
           "AND vc.code = :code " +
           "AND vc.used = false " +
           "AND vc.expiresAt > :now")
    Optional<VerificationCode> findValidCode(
            @Param("email") String email,
            @Param("code") String code,
            @Param("now") LocalDateTime now);

    /**
     * Invalida (marca como usados) todos los códigos anteriores de un usuario.
     */
    @Modifying
    @Query("UPDATE VerificationCode vc SET vc.used = true " +
           "WHERE vc.usuario = :usuario AND vc.used = false")
    void invalidateAllByUsuario(@Param("usuario") Usuario usuario);

    /**
     * Cuenta cuántos códigos se han generado para un usuario en las últimas N horas.
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc " +
           "WHERE vc.usuario = :usuario " +
           "AND vc.createdAt > :since")
    long countRecentCodes(
            @Param("usuario") Usuario usuario,
            @Param("since") LocalDateTime since);
}
