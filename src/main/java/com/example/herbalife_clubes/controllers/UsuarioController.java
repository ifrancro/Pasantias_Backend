package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;
import com.example.herbalife_clubes.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin("*")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/perfil/{usuarioId}")
    public ResponseEntity<UsuarioDTO> getPerfil(@PathVariable Integer usuarioId) {
        UsuarioDTO usuarioDTO = usuarioService.getPerfil(usuarioId);
        return ResponseEntity.ok(usuarioDTO);
    }

    @PutMapping("/perfil/{usuarioId}")
    public ResponseEntity<UsuarioDTO> actualizarPerfil(@PathVariable Integer usuarioId,
                                                         @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO updatedUsuarioDTO = usuarioService.actualizarPerfil(usuarioId, usuarioDTO);
        return ResponseEntity.ok(updatedUsuarioDTO);
    }

    @PatchMapping("{id}/desactivar")
    public ResponseEntity<UsuarioDTO> desactivarUsuario(@PathVariable Integer id) {
        UsuarioDTO usuarioDTO = usuarioService.desactivarUsuario(id);
        return ResponseEntity.ok(usuarioDTO);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("{id}")
    public ResponseEntity<UsuarioDTO> getUsuario(@PathVariable Integer id) {
        UsuarioDTO usuarioDTO = usuarioService.getUsuario(id);
        return ResponseEntity.ok(usuarioDTO);
    }
}

