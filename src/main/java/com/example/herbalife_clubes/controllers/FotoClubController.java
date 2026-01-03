package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.fotoclub.FotoClubDTO;
import com.example.herbalife_clubes.services.FotoClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fotos-club")
@CrossOrigin("*")
public class FotoClubController {
    @Autowired
    private FotoClubService fotoClubService;

    @PostMapping("/subir")
    public ResponseEntity<FotoClubDTO> subirFoto(@RequestParam Integer clubId,
                                                  @RequestParam String urlFoto,
                                                  @RequestParam(required = false) String tipo) {
        FotoClubDTO fotoDTO = fotoClubService.subirFoto(clubId, urlFoto, tipo);
        return new ResponseEntity<>(fotoDTO, HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> eliminarFoto(@PathVariable Integer id) {
        fotoClubService.eliminarFoto(id);
        return ResponseEntity.ok("Foto eliminada exitosamente");
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<FotoClubDTO>> listarFotosByClub(@PathVariable Integer clubId) {
        List<FotoClubDTO> fotos = fotoClubService.listarFotosByClub(clubId);
        return ResponseEntity.ok(fotos);
    }
}

