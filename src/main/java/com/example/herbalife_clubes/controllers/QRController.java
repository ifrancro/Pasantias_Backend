package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.qr.QRValidacionRequest;
import com.example.herbalife_clubes.dtos.qr.QRValidacionResponse;
import com.example.herbalife_clubes.services.QRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
public class QRController {
    @Autowired
    private QRService qrService;

    @PostMapping("/validar-socio")
    public ResponseEntity<QRValidacionResponse> validarSocio(@RequestBody QRValidacionRequest request) {
        QRValidacionResponse response = qrService.validarSocio(request);
        
        if (response.getValido()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}

