package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.sabor.SaborDTO;
import com.example.herbalife_clubes.dtos.sabor.SaborDisponibilidadDTO;
import com.example.herbalife_clubes.entities.Usuario;

import java.util.List;

public interface SaborService {

    // --- Catálogo de sabores (Admin) ---
    List<SaborDTO> getSaboresByHub(Integer hubId);
    SaborDTO createSabor(SaborDTO saborDTO);
    SaborDTO updateSabor(Integer saborId, SaborDTO saborDTO);

    // --- Sabores asignados a un producto (Admin o anfitrión según producto) ---
    List<SaborDTO> getSaboresDeProducto(Integer productoId);
    void asignarSaborAProducto(Integer productoId, Integer saborId, Usuario usuario);
    void quitarSaborDeProducto(Integer productoId, Integer saborId, Usuario usuario);

    // --- Disponibilidad por club (Anfitrión) ---
    List<SaborDisponibilidadDTO> getSaboresDeProductoEnClub(Integer clubId, Integer productoId);
    /** Todos los sabores del hub con disponibilidad en el club (para gestión del anfitrión). */
    List<SaborDisponibilidadDTO> getSaboresGestionEnClub(Integer clubId, Integer productoId);
    SaborDisponibilidadDTO toggleSaborEnClub(Integer clubId, Integer productoId, Integer saborId, Usuario usuario);
}
