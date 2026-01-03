package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.nivelsocio.NivelSocioDTO;
import com.example.herbalife_clubes.services.NivelSocioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/niveles-socio")
@CrossOrigin("*")
public class NivelSocioController {
    @Autowired
    private NivelSocioService nivelSocioService;

    @PostMapping
    public ResponseEntity<NivelSocioDTO> createNivelSocio(@RequestBody NivelSocioDTO nivelSocioDTO) {
        NivelSocioDTO savedNivelDTO = nivelSocioService.createNivelSocio(nivelSocioDTO);
        return new ResponseEntity<>(savedNivelDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<NivelSocioDTO>> getNivelesSocio() {
        List<NivelSocioDTO> niveles = nivelSocioService.getNivelesSocio();
        return ResponseEntity.ok(niveles);
    }

    @GetMapping("{id}")
    public ResponseEntity<NivelSocioDTO> getNivelSocio(@PathVariable Integer id) {
        NivelSocioDTO nivelDTO = nivelSocioService.getNivelSocio(id);
        return ResponseEntity.ok(nivelDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<NivelSocioDTO> updateNivelSocio(@PathVariable Integer id, @RequestBody NivelSocioDTO nivelSocioDTO) {
        NivelSocioDTO updatedNivelDTO = nivelSocioService.updateNivelSocio(id, nivelSocioDTO);
        return ResponseEntity.ok(updatedNivelDTO);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteNivelSocio(@PathVariable Integer id) {
        nivelSocioService.deleteNivelSocio(id);
        return ResponseEntity.ok("Nivel socio eliminado exitosamente");
    }
}

