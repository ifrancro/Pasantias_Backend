package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HubRepository extends JpaRepository<Hub, Integer> {
}

