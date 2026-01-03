package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.logro.LogroDTO;
import com.example.herbalife_clubes.services.LogroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logros")
@CrossOrigin("*")
public class LogroController {
    @Autowired
    private LogroService logroService;

    @PostMapping
    public ResponseEntity<LogroDTO> createLogro(@RequestBody LogroDTO logroDTO) {
        LogroDTO savedLogroDTO = logroService.createLogro(logroDTO);
        return new ResponseEntity<>(savedLogroDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LogroDTO>> getLogros() {
        List<LogroDTO> logros = logroService.getLogros();
        return ResponseEntity.ok(logros);
    }

    @GetMapping("{id}")
    public ResponseEntity<LogroDTO> getLogro(@PathVariable Integer id) {
        LogroDTO logroDTO = logroService.getLogro(id);
        return ResponseEntity.ok(logroDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<LogroDTO> updateLogro(@PathVariable Integer id, @RequestBody LogroDTO logroDTO) {
        LogroDTO updatedLogroDTO = logroService.updateLogro(id, logroDTO);
        return ResponseEntity.ok(updatedLogroDTO);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteLogro(@PathVariable Integer id) {
        logroService.deleteLogro(id);
        return ResponseEntity.ok("Logro eliminado exitosamente");
    }

    @PatchMapping("{id}/activar")
    public ResponseEntity<LogroDTO> activarLogro(@PathVariable Integer id) {
        LogroDTO logroDTO = logroService.activarLogro(id);
        return ResponseEntity.ok(logroDTO);
    }

    @PatchMapping("{id}/inactivar")
    public ResponseEntity<LogroDTO> inactivarLogro(@PathVariable Integer id) {
        LogroDTO logroDTO = logroService.inactivarLogro(id);
        return ResponseEntity.ok(logroDTO);
    }
}

