package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.hub.HubDTO;
import com.example.herbalife_clubes.services.HubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
@CrossOrigin("*")
public class HubController {
    @Autowired
    private HubService hubService;

    @PostMapping
    public ResponseEntity<HubDTO> createHub(@RequestBody HubDTO hubDTO, 
                                           @RequestParam(required = false) Integer adminId) {
        // Si no se proporciona adminId, intentar obtenerlo del usuario autenticado
        if (adminId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // Aquí podrías obtener el ID del usuario autenticado
            // Por ahora requerimos que se pase explícitamente
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        HubDTO savedHubDTO = hubService.createHub(hubDTO, adminId);
        return new ResponseEntity<>(savedHubDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<HubDTO>> getAllHubs() {
        List<HubDTO> hubs = hubService.getAllHubs();
        return ResponseEntity.ok(hubs);
    }

    @GetMapping("{id}")
    public ResponseEntity<HubDTO> getHub(@PathVariable Integer id) {
        HubDTO hubDTO = hubService.getHub(id);
        return ResponseEntity.ok(hubDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<HubDTO> updateHub(@PathVariable Integer id, @RequestBody HubDTO hubDTO) {
        HubDTO updatedHubDTO = hubService.updateHub(id, hubDTO);
        return ResponseEntity.ok(updatedHubDTO);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> deleteHub(@PathVariable Integer id) {
        hubService.deleteHub(id);
        return ResponseEntity.ok("Hub eliminado exitosamente");
    }

    @PatchMapping("{id}/activar")
    public ResponseEntity<HubDTO> activarHub(@PathVariable Integer id) {
        HubDTO hubDTO = hubService.activarHub(id);
        return ResponseEntity.ok(hubDTO);
    }

    @PatchMapping("{id}/inactivar")
    public ResponseEntity<HubDTO> inactivarHub(@PathVariable Integer id) {
        HubDTO hubDTO = hubService.inactivarHub(id);
        return ResponseEntity.ok(hubDTO);
    }
}

