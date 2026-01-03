package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.MembresiaLogro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaLogroRepository extends JpaRepository<MembresiaLogro, Integer> {
    List<MembresiaLogro> findByMembresiaId(Integer membresiaId);
    Optional<MembresiaLogro> findByMembresiaIdAndLogroId(Integer membresiaId, Integer logroId);
}

