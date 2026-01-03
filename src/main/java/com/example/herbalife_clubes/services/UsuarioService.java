package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;

import java.util.List;

public interface UsuarioService {
    UsuarioDTO getPerfil(Integer usuarioId);
    UsuarioDTO actualizarPerfil(Integer usuarioId, UsuarioDTO usuarioDTO);
    UsuarioDTO desactivarUsuario(Integer usuarioId);
    List<UsuarioDTO> listarUsuarios();
    UsuarioDTO getUsuario(Integer usuarioId);
}

