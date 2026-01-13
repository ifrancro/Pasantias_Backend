package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin("*")
public class RolController {

    @Autowired
    private RolRepository rolRepository;

    @GetMapping
    public ResponseEntity<List<Rol>> getAllRoles() {
        List<Rol> roles = rolRepository.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("{id}")
    public ResponseEntity<Rol> getRol(@PathVariable Integer id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + id));
        return ResponseEntity.ok(rol);
    }
}

