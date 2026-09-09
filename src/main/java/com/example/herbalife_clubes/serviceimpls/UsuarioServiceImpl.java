package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ConflictException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.UsuarioMapper;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_INACTIVO = "INACTIVO";
    private static final String ESTADO_PENDIENTE_VERIFICACION = "PENDIENTE_VERIFICACION";
    private static final String ESTADO_BLOQUEADO = "BLOQUEADO";
    private static final String ROL_ADMIN = "ADMIN";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UsuarioDTO getPerfil(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        return UsuarioMapper.mapUsuarioToUsuarioDTO(usuario);
    }

    @Override
    public UsuarioDTO actualizarPerfil(Integer usuarioId, UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setFechaNacimiento(usuarioDTO.getFechaNacimiento());
        usuario.setRedesSociales(usuarioDTO.getRedesSociales());
        
        Usuario updatedUsuario = usuarioRepository.save(usuario);
        return UsuarioMapper.mapUsuarioToUsuarioDTO(updatedUsuario);
    }

    /**
     * Baja administrativa: solo ACTIVO -> INACTIVO.
     *
     * Se valida el estado de origen a propósito: dar de baja una cuenta en
     * PENDIENTE_VERIFICACION la sacaría del barrido de registros vencidos
     * (UsuarioRepository.findPendientesVencidos) y su email quedaría ocupado
     * para siempre en la columna UNIQUE.
     */
    @Override
    @Transactional
    public UsuarioDTO desactivarUsuario(Integer usuarioId, Integer adminId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        if (usuarioId.equals(adminId)) {
            throw new ConflictException("No puedes desactivar tu propia cuenta");
        }

        if (esAdmin(usuario)) {
            throw new ConflictException("No se puede desactivar una cuenta ADMIN");
        }

        // estado null se trata como ACTIVO, igual que en Usuario.isEnabled()
        String estadoActual = usuario.getEstado();
        if (estadoActual != null && !ESTADO_ACTIVO.equalsIgnoreCase(estadoActual)) {
            throw new ConflictException(mensajeDesactivacionInvalida(estadoActual));
        }

        usuario.setEstado(ESTADO_INACTIVO);
        Usuario updatedUsuario = usuarioRepository.save(usuario);
        return UsuarioMapper.mapUsuarioToUsuarioDTO(updatedUsuario);
    }

    /**
     * Alta administrativa: solo INACTIVO -> ACTIVO.
     *
     * No sirve para saltarse la verificación de correo (PENDIENTE_VERIFICACION)
     * ni para levantar una sanción (BLOQUEADO): son decisiones de negocio distintas.
     */
    @Override
    @Transactional
    public UsuarioDTO reactivarUsuario(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));

        String estadoActual = usuario.getEstado();
        if (!ESTADO_INACTIVO.equalsIgnoreCase(estadoActual)) {
            throw new ConflictException(mensajeReactivacionInvalida(estadoActual));
        }

        usuario.setEstado(ESTADO_ACTIVO);
        Usuario updatedUsuario = usuarioRepository.save(usuario);
        return UsuarioMapper.mapUsuarioToUsuarioDTO(updatedUsuario);
    }

    @Override
    public List<UsuarioDTO> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarios.stream()
                .map(UsuarioMapper::mapUsuarioToUsuarioDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDTO getUsuario(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + usuarioId));
        return UsuarioMapper.mapUsuarioToUsuarioDTO(usuario);
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario.getRol() != null && ROL_ADMIN.equalsIgnoreCase(usuario.getRol().getNombre());
    }

    private String mensajeDesactivacionInvalida(String estadoActual) {
        if (ESTADO_INACTIVO.equalsIgnoreCase(estadoActual)) {
            return "El usuario ya está inactivo";
        }
        if (ESTADO_PENDIENTE_VERIFICACION.equalsIgnoreCase(estadoActual)) {
            return "El usuario no ha verificado su correo; no se puede desactivar una cuenta pendiente de verificación";
        }
        if (ESTADO_BLOQUEADO.equalsIgnoreCase(estadoActual)) {
            return "La cuenta está bloqueada; no requiere desactivación";
        }
        return "Solo se puede desactivar una cuenta ACTIVA. Estado actual: " + estadoActual;
    }

    private String mensajeReactivacionInvalida(String estadoActual) {
        if (ESTADO_ACTIVO.equalsIgnoreCase(estadoActual) || estadoActual == null) {
            return "El usuario ya está activo";
        }
        if (ESTADO_PENDIENTE_VERIFICACION.equalsIgnoreCase(estadoActual)) {
            return "El usuario debe verificar su correo para activar su cuenta";
        }
        if (ESTADO_BLOQUEADO.equalsIgnoreCase(estadoActual)) {
            return "La cuenta está bloqueada; debe desbloquearse antes de reactivarla";
        }
        return "Solo se puede reactivar una cuenta INACTIVA. Estado actual: " + estadoActual;
    }
}
