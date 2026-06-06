package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboItemRepository extends JpaRepository<ComboItem, Integer> {
    List<ComboItem> findByComboId(Integer comboId);
    void deleteByComboId(Integer comboId);
}
