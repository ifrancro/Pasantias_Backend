package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.FotoClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoClubRepository extends JpaRepository<FotoClub, Integer> {
    List<FotoClub> findByClubId(Integer clubId);
}

