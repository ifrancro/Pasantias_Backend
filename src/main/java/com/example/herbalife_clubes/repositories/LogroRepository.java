package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Logro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogroRepository extends JpaRepository<Logro, Integer> {

    @Query("SELECT DISTINCT l FROM Logro l LEFT JOIN FETCH l.requisitos WHERE l.id = :id")
    Optional<Logro> findByIdWithRequisitos(@Param("id") Integer id);

    @Query("SELECT DISTINCT l FROM Logro l LEFT JOIN FETCH l.requisitos")
    List<Logro> findAllWithRequisitos();
}

