package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;

import java.util.List;

public interface UsuarioService {
    UsuarioDTO getPerfil(Integer usuarioId);
    UsuarioDTO actualizarPerfil(Integer usuarioId, UsuarioDTO usuarioDTO);

    /**
     * Baja administrativa de una cuenta: ACTIVO -> INACTIVO.
     *
     * @param usuarioId cuenta a dar de baja
     * @param adminId   admin autenticado que ejecuta la acción
     */
    UsuarioDTO desactivarUsuario(Integer usuarioId, Integer adminId);

    /**
     * Alta administrativa de una cuenta dada de baja: INACTIVO -> ACTIVO.
     * No sustituye a la verificación de correo ni al desbloqueo de una cuenta sancionada.
     */
    UsuarioDTO reactivarUsuario(Integer usuarioId);

    List<UsuarioDTO> listarUsuarios();
    UsuarioDTO getUsuario(Integer usuarioId);
}
