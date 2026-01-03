package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.hub.HubDTO;

import java.util.List;

public interface HubService {
    HubDTO createHub(HubDTO hubDTO, Integer adminId);
    HubDTO updateHub(Integer hubId, HubDTO hubDTO);
    String deleteHub(Integer hubId);
    HubDTO getHub(Integer hubId);
    List<HubDTO> getAllHubs();
    HubDTO activarHub(Integer hubId);
    HubDTO inactivarHub(Integer hubId);
}

