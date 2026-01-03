package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.entities.MembresiaLogro;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresia-logros")
@CrossOrigin("*")
public class MembresiaLogroController {
    @Autowired
    private MembresiaLogroService membresiaLogroService;

    @PostMapping("/evaluar/{membresiaId}")
    public ResponseEntity<String> evaluarLogros(@PathVariable Integer membresiaId) {
        membresiaLogroService.evaluarLogrosAutomaticamente(membresiaId);
        return ResponseEntity.ok("Logros evaluados automáticamente");
    }

    @GetMapping("/membresia/{membresiaId}")
    public ResponseEntity<List<MembresiaLogro>> listarLogrosByMembresia(@PathVariable Integer membresiaId) {
        List<MembresiaLogro> logros = membresiaLogroService.listarLogrosByMembresia(membresiaId);
        return ResponseEntity.ok(logros);
    }
}

