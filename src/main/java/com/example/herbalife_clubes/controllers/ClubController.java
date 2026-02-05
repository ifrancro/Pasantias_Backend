package com.example.herbalife_clubes.controllers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.services.ClubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubes")
@CrossOrigin("*")
public class ClubController {
    @Autowired
    private ClubService clubService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<ClubDTO> createClub(@RequestBody ClubDTO clubDTO,
                                               @RequestParam Integer hubId,
                                               @RequestParam Integer anfitrionId) {
        ClubDTO savedClubDTO = clubService.createClub(clubDTO, hubId, anfitrionId);
        return new ResponseEntity<>(savedClubDTO, HttpStatus.CREATED);
    }

    @PostMapping("/solicitud")
    public ResponseEntity<ClubDTO> crearSolicitudClub(@RequestBody ClubDTO clubDTO) {
        // Validar que el payload tenga los campos requeridos
        if (clubDTO.getAnfitrionId() == null || clubDTO.getHubId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Asegurar que el estado sea PENDIENTE
        clubDTO.setEstado("PENDIENTE");
        
        ClubDTO savedClubDTO = clubService.createClub(
            clubDTO, 
            clubDTO.getHubId(), 
            clubDTO.getAnfitrionId()
        );
        return new ResponseEntity<>(savedClubDTO, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ClubDTO>> getAllClubes(@RequestParam(required = false) Integer hubId) {
        List<ClubDTO> clubes;
        if (hubId != null) {
            clubes = clubService.getClubesByHub(hubId);
        } else {
            clubes = clubService.getAllClubes();
        }
        return ResponseEntity.ok(clubes);
    }

    @GetMapping("{id}")
    public ResponseEntity<ClubDTO> getClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.getClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PutMapping("{id}")
    public ResponseEntity<ClubDTO> updateClub(@PathVariable Integer id, @RequestBody ClubDTO clubDTO) {
        ClubDTO updatedClubDTO = clubService.updateClub(id, clubDTO);
        return ResponseEntity.ok(updatedClubDTO);
    }

    @PatchMapping("{id}/aprobar")
    public ResponseEntity<ClubDTO> aprobarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.aprobarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PatchMapping("{id}/rechazar")
    public ResponseEntity<ClubDTO> rechazarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.rechazarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PatchMapping("{id}/activar")
    public ResponseEntity<ClubDTO> activarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.activarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    @PatchMapping("{id}/desactivar")
    public ResponseEntity<ClubDTO> desactivarClub(@PathVariable Integer id) {
        ClubDTO clubDTO = clubService.desactivarClub(id);
        return ResponseEntity.ok(clubDTO);
    }

    /**
     * Obtiene el club del anfitrión autenticado.
     * Endpoint: GET /api/clubes/mio
     * 
     * Busca el club donde clubes.anfitrion_id == usuario_autenticado.id
     * 
     * @return ClubDTO del club del anfitrión autenticado
     */
    @GetMapping("/mio")
    public ResponseEntity<ClubDTO> getMiClub() {
        System.out.println("[JSON DEBUG] ===== GET /api/clubes/mio =====");
        
        // Obtener usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            System.out.println("[JSON DEBUG] ERROR: No autenticado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElse(null);

        if (usuario == null) {
            System.out.println("[JSON DEBUG] ERROR: Usuario no encontrado con email: " + email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        System.out.println("[JSON DEBUG] Usuario autenticado - ID: " + usuario.getId() + ", Email: " + email);
        
        ClubDTO clubDTO = clubService.getClubByAnfitrion(usuario.getId());
        
        // Imprimir JSON real
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(clubDTO);
            System.out.println("[JSON DEBUG] JSON Response (GET /api/clubes/mio):");
            System.out.println(json);
            
            // Imprimir detalles de campos con tipos
            System.out.println("[JSON DEBUG] Detalles de campos:");
            System.out.println("  - id: " + clubDTO.getId() + " (tipo: " + (clubDTO.getId() != null ? clubDTO.getId().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - hubId: " + clubDTO.getHubId() + " (tipo: " + (clubDTO.getHubId() != null ? clubDTO.getHubId().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - hubNombre: " + clubDTO.getHubNombre() + " (tipo: " + (clubDTO.getHubNombre() != null ? clubDTO.getHubNombre().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - anfitrionId: " + clubDTO.getAnfitrionId() + " (tipo: " + (clubDTO.getAnfitrionId() != null ? clubDTO.getAnfitrionId().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - anfitrionNombre: " + clubDTO.getAnfitrionNombre() + " (tipo: " + (clubDTO.getAnfitrionNombre() != null ? clubDTO.getAnfitrionNombre().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - nombreClub: " + clubDTO.getNombreClub() + " (tipo: " + (clubDTO.getNombreClub() != null ? clubDTO.getNombreClub().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - direccion: " + clubDTO.getDireccion() + " (tipo: " + (clubDTO.getDireccion() != null ? clubDTO.getDireccion().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - horario: " + clubDTO.getHorario() + " (tipo: " + (clubDTO.getHorario() != null ? clubDTO.getHorario().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - lat: " + clubDTO.getLat() + " (tipo: " + (clubDTO.getLat() != null ? clubDTO.getLat().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - lng: " + clubDTO.getLng() + " (tipo: " + (clubDTO.getLng() != null ? clubDTO.getLng().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - estado: " + clubDTO.getEstado() + " (tipo: " + (clubDTO.getEstado() != null ? clubDTO.getEstado().getClass().getSimpleName() : "null") + ")");
            System.out.println("  - createdAt: " + clubDTO.getCreatedAt() + " (tipo: " + (clubDTO.getCreatedAt() != null ? clubDTO.getCreatedAt().getClass().getSimpleName() : "null") + ")");
            
            System.out.println("[JSON DEBUG] ===== FIN GET /api/clubes/mio =====");
        } catch (Exception e) {
            System.out.println("[JSON DEBUG] ERROR al serializar JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(clubDTO);
    }
}

