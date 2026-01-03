package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;
import com.example.herbalife_clubes.entities.Usuario;

public class UsuarioMapper {
    public static UsuarioDTO mapUsuarioToUsuarioDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setRolId(usuario.getRol() != null ? usuario.getRol().getId() : null);
        dto.setRolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setTelefono(usuario.getTelefono());
        dto.setFechaNacimiento(usuario.getFechaNacimiento());
        dto.setRedesSociales(usuario.getRedesSociales());
        dto.setEstado(usuario.getEstado());
        dto.setCreatedAt(usuario.getCreatedAt());
        return dto;
    }

    public static Usuario mapUsuarioDTOToUsuario(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setId(dto.getId());
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setFechaNacimiento(dto.getFechaNacimiento());
        usuario.setRedesSociales(dto.getRedesSociales());
        usuario.setEstado(dto.getEstado());
        return usuario;
    }
}

