package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Registros que nunca completaron la verificación de correo y ya vencieron.
     * Ocupan el email (columna UNIQUE) impidiendo que la persona se re-registre.
     */
    @Query("SELECT u FROM Usuario u WHERE u.estado = 'PENDIENTE_VERIFICACION' AND u.createdAt < :limite")
    List<Usuario> findPendientesVencidos(@Param("limite") LocalDateTime limite);
}

