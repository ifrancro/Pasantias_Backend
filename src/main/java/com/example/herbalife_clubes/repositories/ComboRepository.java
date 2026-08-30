package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComboRepository extends JpaRepository<Combo, Integer> {
    List<Combo> findByClubId(Integer clubId);
    List<Combo> findByClubIdAndActivoTrue(Integer clubId);

    @Query("SELECT DISTINCT c FROM Combo c "
            + "LEFT JOIN FETCH c.items ci "
            + "LEFT JOIN FETCH ci.producto "
            + "LEFT JOIN FETCH c.club "
            + "WHERE c.id = :id")
    Optional<Combo> findByIdWithItems(@Param("id") Integer id);
}
