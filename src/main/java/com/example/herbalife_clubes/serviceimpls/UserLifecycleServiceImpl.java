package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.dtos.membresia.*;
import com.example.herbalife_clubes.dtos.prospecto.*;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ConflictException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.mappers.AsistenciaMapper;
import com.example.herbalife_clubes.repositories.*;
import com.example.herbalife_clubes.services.UserLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserLifecycleServiceImpl implements UserLifecycleService {

    private static final String ESTADO_EN_SEGUIMIENTO = "EN_SEGUIMIENTO";
    private static final String ESTADO_CONVERTIDO = "CONVERTIDO";

    private final ProspectoRepository prospectoRepository;
    private final MisionProspectoRepository misionProspectoRepository;
    private final CompraManualRepository compraManualRepository;
    private final ClubRepository clubRepository;
    private final MembresiaRepository membresiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsistenciaRepository asistenciaRepository;

    @Override
    @Transactional
    public ProspectoDTO crearProspecto(Integer clubId, ProspectoCreateRequest request) {
        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(clubId, anfitrion.getId());

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club no encontrado con id: " + clubId));

        Membresia referidoPor = null;
        if (request.getReferidoPorMembresiaId() != null) {
            referidoPor = membresiaRepository.findById(request.getReferidoPorMembresiaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membresía referente no encontrada con id: " + request.getReferidoPorMembresiaId()));
            if (!clubId.equals(referidoPor.getClub().getId())) {
                throw new IllegalArgumentException("La membresía referente debe pertenecer al mismo club");
            }
        }

        Prospecto p = new Prospecto();
        p.setClub(club);
        p.setNombre(request.getNombre().trim());
        p.setTelefono(request.getTelefono().trim());
        p.setReferidoPorMembresia(referidoPor);
        p.setEstado(ESTADO_EN_SEGUIMIENTO);

        Prospecto saved = prospectoRepository.save(p);
        return toProspectoDTO(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProspectoDTO> listarProspectos(Integer clubId) {
        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(clubId, anfitrion.getId());

        List<Prospecto> prospectos = prospectoRepository.findByClubIdOrderByFechaCreacionDescIdDesc(clubId);
        if (prospectos.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = prospectos.stream().map(Prospecto::getId).toList();
        Map<Integer, List<MisionProspecto>> misionesPorProspecto = ids.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        misionProspectoRepository::findByProspectoIdOrderByIdAsc
                ));

        return prospectos.stream()
                .map(p -> toProspectoDTO(p, misionesPorProspecto.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public ProspectoDTO actualizarEstadoProspecto(Integer prospectoId, ProspectoEstadoUpdateRequest request) {
        Prospecto prospecto = prospectoRepository.findById(prospectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospecto no encontrado con id: " + prospectoId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(prospecto.getClub().getId(), anfitrion.getId());

        String estado = request.getEstado() != null ? request.getEstado().trim().toUpperCase() : "";
        if (!ESTADO_EN_SEGUIMIENTO.equals(estado) && !ESTADO_CONVERTIDO.equals(estado)) {
            throw new IllegalArgumentException("estado debe ser EN_SEGUIMIENTO o CONVERTIDO");
        }

        prospecto.setEstado(estado);
        Prospecto saved = prospectoRepository.save(prospecto);
        List<MisionProspecto> misiones = misionProspectoRepository.findByProspectoIdOrderByIdAsc(saved.getId());
        return toProspectoDTO(saved, misiones);
    }

    @Override
    @Transactional
    public MisionProspectoDTO crearMision(Integer prospectoId, MisionProspectoCreateRequest request) {
        Prospecto prospecto = prospectoRepository.findById(prospectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospecto no encontrado con id: " + prospectoId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(prospecto.getClub().getId(), anfitrion.getId());

        MisionProspecto m = new MisionProspecto();
        m.setProspecto(prospecto);
        m.setNombre(request.getNombre().trim());
        m.setDescripcion(request.getDescripcion());
        m.setMetaCantidad(request.getMetaCantidad());
        m.setProgresoActual(0);
        m.setFechaLimite(request.getFechaLimite());
        MisionProspecto saved = misionProspectoRepository.save(m);
        return toMisionDTO(saved);
    }

    @Override
    @Transactional
    public MisionProspectoDTO incrementarProgresoMision(Integer misionId) {
        MisionProspecto mision = misionProspectoRepository.findById(misionId)
                .orElseThrow(() -> new ResourceNotFoundException("Misión no encontrada con id: " + misionId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(mision.getProspecto().getClub().getId(), anfitrion.getId());

        if (mision.getProgresoActual() >= mision.getMetaCantidad()) {
            throw new IllegalArgumentException("La misión ya está completada");
        }

        mision.setProgresoActual(mision.getProgresoActual() + 1);
        MisionProspecto saved = misionProspectoRepository.save(mision);
        return toMisionDTO(saved);
    }

    @Override
    @Transactional
    public void eliminarMision(Integer misionId) {
        MisionProspecto mision = misionProspectoRepository.findById(misionId)
                .orElseThrow(() -> new ResourceNotFoundException("Misión no encontrada con id: " + misionId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(mision.getProspecto().getClub().getId(), anfitrion.getId());

        misionProspectoRepository.delete(mision);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraManualDTO> listarCompras(Integer membresiaId) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(membresia.getClub().getId(), anfitrion.getId());

        return compraManualRepository.findByMembresiaIdOrderByFechaDescIdDesc(membresiaId).stream()
                .map(this::toCompraDTO)
                .toList();
    }

    @Override
    @Transactional
    public CompraManualDTO crearCompra(Integer membresiaId, CompraManualCreateRequest request) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(membresia.getClub().getId(), anfitrion.getId());

        CompraManual compra = new CompraManual();
        compra.setMembresia(membresia);
        compra.setClub(membresia.getClub());
        compra.setDescripcion(request.getDescripcion().trim());
        compra.setMonto(request.getMonto());
        compra.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        compra.setRegistradaPorHost(anfitrion);

        return toCompraDTO(compraManualRepository.save(compra));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferidoSocioDTO> listarReferidos(Integer membresiaId) {
        Membresia raiz = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(raiz.getClub().getId(), anfitrion.getId());

        return membresiaRepository.findByReferidoPorMembresiaId(membresiaId).stream()
                .map(this::toReferidoDTO)
                .toList();
    }

    @Override
    @Transactional
    public AsistenciaDTO registrarAsistenciaManual(Integer membresiaId, AsistenciaManualCreateRequest request) {
        Membresia membresia = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId));

        Usuario anfitrion = getUsuarioAutenticado();
        validarAnfitrionDeClub(membresia.getClub().getId(), anfitrion.getId());

        LocalDate fecha = request.getFecha() != null ? request.getFecha() : LocalDate.now();
        if (asistenciaRepository.findByMembresiaIdAndFechaDia(membresiaId, fecha).isPresent()) {
            throw new ConflictException("Ya existe una asistencia para la membresía en la fecha " + fecha);
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setMembresia(membresia);
        asistencia.setClub(membresia.getClub());
        asistencia.setFechaDia(fecha);
        asistencia.setEstado("PRESENTE");
        asistencia.setNota(request.getNota());

        Asistencia saved = asistenciaRepository.save(asistencia);
        return AsistenciaMapper.mapAsistenciaToAsistenciaDTO(saved);
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado"));
    }

    private void validarAnfitrionDeClub(Integer clubId, Integer usuarioId) {
        boolean esAnfitrion = clubRepository.findByIdAndAnfitrionId(clubId, usuarioId).isPresent();
        if (!esAnfitrion) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos sobre este club");
        }
    }

    private ProspectoDTO toProspectoDTO(Prospecto p, List<MisionProspecto> misiones) {
        return ProspectoDTO.builder()
                .id(p.getId())
                .clubId(p.getClub().getId())
                .nombre(p.getNombre())
                .telefono(p.getTelefono())
                .referidoPorMembresiaId(p.getReferidoPorMembresia() != null ? p.getReferidoPorMembresia().getId() : null)
                .referidoPorNombre(getNombreMembresia(p.getReferidoPorMembresia()))
                .fechaCreacion(p.getFechaCreacion())
                .estado(p.getEstado())
                .misiones(misiones.stream().map(this::toMisionDTO).toList())
                .build();
    }

    private MisionProspectoDTO toMisionDTO(MisionProspecto m) {
        int progreso = m.getProgresoActual() != null ? m.getProgresoActual() : 0;
        int meta = m.getMetaCantidad() != null ? m.getMetaCantidad() : 0;
        return MisionProspectoDTO.builder()
                .id(m.getId())
                .prospectoId(m.getProspecto().getId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .metaCantidad(meta)
                .progresoActual(progreso)
                .fechaLimite(m.getFechaLimite())
                .completada(progreso >= meta)
                .build();
    }

    private CompraManualDTO toCompraDTO(CompraManual c) {
        return CompraManualDTO.builder()
                .id(c.getId())
                .membresiaId(c.getMembresia().getId())
                .clubId(c.getClub().getId())
                .descripcion(c.getDescripcion())
                .monto(c.getMonto())
                .fecha(c.getFecha())
                .registradaPorHostId(c.getRegistradaPorHost().getId())
                .build();
    }

    private ReferidoSocioDTO toReferidoDTO(Membresia m) {
        String nombre = null;
        if (m.getUsuario() != null) {
            String n = m.getUsuario().getNombre() != null ? m.getUsuario().getNombre() : "";
            String a = m.getUsuario().getApellido() != null ? m.getUsuario().getApellido() : "";
            nombre = (n + " " + a).trim();
        }
        return ReferidoSocioDTO.builder()
                .id(m.getId())
                .usuarioId(m.getUsuario() != null ? m.getUsuario().getId() : null)
                .usuarioNombre(nombre)
                .clubId(m.getClub() != null ? m.getClub().getId() : null)
                .clubNombre(m.getClub() != null ? m.getClub().getNombreClub() : null)
                .nivelId(m.getNivel() != null ? m.getNivel().getId() : null)
                .nivelNombre(m.getNivel() != null ? m.getNivel().getNombre() : null)
                .numeroSocio(m.getNumeroSocio())
                .puntosAcumulados(m.getPuntosAcumulados())
                .fechaRegistro(m.getFechaRegistro() != null ? m.getFechaRegistro().toLocalDate() : null)
                .estado(m.getEstado())
                .build();
    }

    private String getNombreMembresia(Membresia m) {
        if (m == null || m.getUsuario() == null) {
            return null;
        }
        String nombre = m.getUsuario().getNombre() != null ? m.getUsuario().getNombre() : "";
        String apellido = m.getUsuario().getApellido() != null ? m.getUsuario().getApellido() : "";
        String fullName = (nombre + " " + apellido).trim();
        return fullName.isBlank() ? null : fullName;
    }
}
