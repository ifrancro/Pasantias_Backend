package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.combo.ComboCreateRequest;
import com.example.herbalife_clubes.dtos.combo.ComboDTO;

import java.util.List;

public interface ComboClubService {
    List<ComboDTO> getCombosByClub(Integer clubId);
    ComboDTO getCombo(Integer comboId);
    ComboDTO createCombo(Integer clubId, ComboCreateRequest request);
    ComboDTO updateCombo(Integer comboId, ComboCreateRequest request);
    ComboDTO toggleCombo(Integer comboId);
    void deleteCombo(Integer comboId);
}
