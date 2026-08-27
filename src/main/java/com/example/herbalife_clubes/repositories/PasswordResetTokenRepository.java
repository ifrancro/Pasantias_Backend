package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.PasswordResetToken;
import com.example.herbalife_clubes.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalseAndExpiresAtAfter(
            String tokenHash,
            LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true "
            + "WHERE t.usuario = :usuario AND t.used = false")
    void invalidateAllPendingByUsuario(@Param("usuario") Usuario usuario);
}
