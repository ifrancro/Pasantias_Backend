package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.consumo.ConsumoDTO;
import com.example.herbalife_clubes.entities.Consumo;

public class ConsumoMapper {
    public static ConsumoDTO mapConsumoToConsumoDTO(Consumo consumo) {
        ConsumoDTO dto = new ConsumoDTO();
        dto.setId(consumo.getId());
        dto.setMembresiaId(consumo.getMembresia() != null ? consumo.getMembresia().getId() : null);
        dto.setMembresiaNumeroSocio(consumo.getMembresia() != null ? consumo.getMembresia().getNumeroSocio() : null);
        dto.setClubId(consumo.getClub() != null ? consumo.getClub().getId() : null);
        dto.setClubNombre(consumo.getClub() != null ? consumo.getClub().getNombreClub() : null);
        dto.setAsistenciaId(consumo.getAsistencia() != null ? consumo.getAsistencia().getId() : null);
        dto.setProductoId(consumo.getProducto() != null ? consumo.getProducto().getId() : null);
        dto.setProductoNombre(consumo.getProducto() != null ? consumo.getProducto().getNombre() : null);
        dto.setDescripcion(consumo.getDescripcion());
        dto.setCantidad(consumo.getCantidad());
        dto.setPrecioRegistrado(consumo.getPrecioRegistrado());
        dto.setFechaHora(consumo.getFechaHora());
        dto.setCreatedAt(consumo.getCreatedAt());
        return dto;
    }
}

