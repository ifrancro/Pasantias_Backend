package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.sabor.SaborDTO;
import com.example.herbalife_clubes.dtos.sabor.SaborDisponibilidadDTO;

import java.util.List;

public interface SaborService {

    // --- Catálogo de sabores (Admin) ---
    List<SaborDTO> getSaboresByHub(Integer hubId);
    SaborDTO createSabor(SaborDTO saborDTO);
    SaborDTO updateSabor(Integer saborId, SaborDTO saborDTO);

    // --- Sabores asignados a un producto (Admin) ---
    List<SaborDTO> getSaboresDeProducto(Integer productoId);
    void asignarSaborAProducto(Integer productoId, Integer saborId);
    void quitarSaborDeProducto(Integer productoId, Integer saborId);

    // --- Disponibilidad por club (Anfitrión) ---
    List<SaborDisponibilidadDTO> getSaboresDeProductoEnClub(Integer clubId, Integer productoId);
    SaborDisponibilidadDTO toggleSaborEnClub(Integer clubId, Integer productoId, Integer saborId);
}
