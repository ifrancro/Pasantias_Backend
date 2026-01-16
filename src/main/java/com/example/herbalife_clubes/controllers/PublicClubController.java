package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.services.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador público para consultar clubes activos sin autenticación
 * Estos endpoints son accesibles para cualquier visitante
 */
@RestController
@RequestMapping("/api/public/clubes")
@CrossOrigin("*")
public class PublicClubController {

    @Autowired
    private ClubService clubService;

    /**
     * Obtiene la lista de todos los clubes activos
     * Público - No requiere autenticación
     * 
     * @return Lista de clubes con estado ACTIVO
     */
    @GetMapping
    public ResponseEntity<List<ClubDTO>> getClubesActivos() {
        List<ClubDTO> clubes = clubService.getClubesActivos();
        return ResponseEntity.ok(clubes);
    }

    /**
     * Obtiene el detalle de un club activo por ID
     * Público - No requiere autenticación
     * 
     * @param id ID del club
     * @return Detalle del club solo si está ACTIVO, caso contrario 404
     */
    @GetMapping("{id}")
    public ResponseEntity<ClubDTO> getClubActivo(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.getClubActivo(id);
        return ResponseEntity.ok(clubDTO);
    }
}

