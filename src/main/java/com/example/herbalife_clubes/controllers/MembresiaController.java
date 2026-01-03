package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.services.MembresiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
@CrossOrigin("*")
public class MembresiaController {
    @Autowired
    private MembresiaService membresiaService;

    @PostMapping
    public ResponseEntity<MembresiaDTO> createMembresia(@RequestBody MembresiaDTO membresiaDTO,
                                                         @RequestParam Integer usuarioId,
                                                         @RequestParam Integer clubId,
                                                         @RequestParam(required = false) Integer nivelId) {
        MembresiaDTO savedMembresiaDTO = membresiaService.createMembresia(membresiaDTO, usuarioId, clubId, nivelId);
        return new ResponseEntity<>(savedMembresiaDTO, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<MembresiaDTO> getMembresia(@PathVariable Integer id) {
        MembresiaDTO membresiaDTO = membresiaService.getMembresia(id);
        return ResponseEntity.ok(membresiaDTO);
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<MembresiaDTO> getMembresiaByUsuario(@PathVariable Integer usuarioId) {
        MembresiaDTO membresiaDTO = membresiaService.getMembresiaByUsuario(usuarioId);
        return ResponseEntity.ok(membresiaDTO);
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<MembresiaDTO>> getMembresiasByClub(@PathVariable Integer clubId) {
        List<MembresiaDTO> membresias = membresiaService.getMembresiasByClub(clubId);
        return ResponseEntity.ok(membresias);
    }

    @PatchMapping("{id}/estado")
    public ResponseEntity<MembresiaDTO> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        MembresiaDTO membresiaDTO = membresiaService.cambiarEstado(id, estado);
        return ResponseEntity.ok(membresiaDTO);
    }

    @PatchMapping("{id}/nivel")
    public ResponseEntity<MembresiaDTO> cambiarNivel(@PathVariable Integer id, @RequestParam Integer nivelId) {
        MembresiaDTO membresiaDTO = membresiaService.cambiarNivel(id, nivelId);
        return ResponseEntity.ok(membresiaDTO);
    }

    @PatchMapping("{id}/puntos")
    public ResponseEntity<MembresiaDTO> actualizarPuntos(@PathVariable Integer id, @RequestParam Integer puntos) {
        MembresiaDTO membresiaDTO = membresiaService.actualizarPuntos(id, puntos);
        return ResponseEntity.ok(membresiaDTO);
    }
}

