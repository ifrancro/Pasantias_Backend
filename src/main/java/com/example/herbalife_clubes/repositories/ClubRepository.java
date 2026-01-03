package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {
    List<Club> findByHubId(Integer hubId);
    List<Club> findByAnfitrionId(Integer anfitrionId);
}

