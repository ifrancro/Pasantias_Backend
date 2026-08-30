package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.producto.ProductoDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoGrupoOpcionDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoOpcionDTO;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;
import com.example.herbalife_clubes.entities.Usuario;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProductoMapper {

    /**
     * Flags independientes de exposición. No reutilizar el mapper ADMIN para SOCIO:
     * ingredientes/revisión y grupos/opciones se deciden por separado.
     */
    public static final class Vista {
        public static final Vista INTERNO = new Vista(true, true, true, false);
        public static final Vista PUBLICO = new Vista(false, false, true, true);

        private final boolean incluirIngredientes;
        private final boolean incluirDatosRevision;
        private final boolean incluirGruposOpciones;
        private final boolean filtrarOpcionesInactivas;

        private Vista(
                boolean incluirIngredientes,
                boolean incluirDatosRevision,
                boolean incluirGruposOpciones,
                boolean filtrarOpcionesInactivas) {
            this.incluirIngredientes = incluirIngredientes;
            this.incluirDatosRevision = incluirDatosRevision;
            this.incluirGruposOpciones = incluirGruposOpciones;
            this.filtrarOpcionesInactivas = filtrarOpcionesInactivas;
        }
    }

    /**
     * Mapea Producto a ProductoDTO incluyendo todos los campos (para uso interno/admin)
     */
    public static ProductoDTO mapProductoToProductoDTO(Producto producto) {
        return mapProductoToProductoDTO(producto, Vista.INTERNO);
    }

    /**
     * Compatibilidad: {@code true} = vista interna (ADMIN/ANFITRION);
     * {@code false} = vista pública (SOCIO): sin ingredientes ni revisión,
     * con gruposOpciones y solo opciones activas.
     */
    public static ProductoDTO mapProductoToProductoDTO(Producto producto, boolean incluirIngredientes) {
        return mapProductoToProductoDTO(producto, incluirIngredientes ? Vista.INTERNO : Vista.PUBLICO);
    }

    public static ProductoDTO mapProductoToProductoDTO(Producto producto, Vista vista) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(producto.getId());
        dto.setHubId(producto.getHub() != null ? producto.getHub().getId() : null);
        dto.setHubNombre(producto.getHub() != null ? producto.getHub().getNombre() : null);
        dto.setClubCreadorId(producto.getClubCreador() != null ? producto.getClubCreador().getId() : null);
        dto.setClubCreadorNombre(producto.getClubCreador() != null ? producto.getClubCreador().getNombreClub() : null);
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setImagenUrl(producto.getImagenUrl());
        if (vista.incluirIngredientes) {
            dto.setIngredientes(producto.getIngredientes());
        }
        dto.setPrecio(producto.getPrecio());
        dto.setPuntosValor(producto.getPuntosValor());
        dto.setTipo(producto.getTipo());
        dto.setEsCombo(Boolean.TRUE.equals(producto.getEsCombo()));
        dto.setEstadoAprobacion(producto.getEstadoAprobacion());
        dto.setActivo(producto.getActivo());
        dto.setCreatedAt(producto.getCreatedAt());
        if (vista.incluirDatosRevision) {
            dto.setComentarioRevision(producto.getComentarioRevision());
            if (producto.getRevisadoPor() != null) {
                dto.setRevisadoPorUsuarioId(producto.getRevisadoPor().getId());
                dto.setRevisadoPorNombre(nombreCompleto(producto.getRevisadoPor()));
            }
            dto.setRevisadoAt(producto.getRevisadoAt());
        }
        if (vista.incluirGruposOpciones) {
            dto.setGruposOpciones(mapGrupos(producto, vista.filtrarOpcionesInactivas));
        }
        return dto;
    }

    /**
     * grupos: orden ASC, id ASC. opciones: orden ASC, id ASC.
     * Si filtrarInactivas, solo ProductoOpcion.activo == true; el grupo se mantiene
     * aunque quede con opciones vacías.
     */
    private static List<ProductoGrupoOpcionDTO> mapGrupos(Producto producto, boolean filtrarInactivas) {
        List<ProductoGrupoOpcion> grupos = producto.getGruposOpciones() == null
                ? List.of()
                : new ArrayList<>(producto.getGruposOpciones());
        grupos.sort(Comparator
                .comparing((ProductoGrupoOpcion g) -> g.getOrden() == null ? 0 : g.getOrden())
                .thenComparing(g -> g.getId() == null ? Integer.MAX_VALUE : g.getId()));
        List<ProductoGrupoOpcionDTO> result = new ArrayList<>();
        for (ProductoGrupoOpcion grupo : grupos) {
            ProductoGrupoOpcionDTO gDto = new ProductoGrupoOpcionDTO();
            gDto.setId(grupo.getId());
            gDto.setNombre(grupo.getNombre());
            gDto.setOrden(grupo.getOrden());
            gDto.setMinSelecciones(grupo.getMinSelecciones());
            gDto.setMaxSelecciones(grupo.getMaxSelecciones());
            gDto.setPermiteRepetir(Boolean.TRUE.equals(grupo.getPermiteRepetir()));
            List<ProductoOpcion> opciones = grupo.getOpciones() == null
                    ? List.of()
                    : new ArrayList<>(grupo.getOpciones());
            opciones.sort(Comparator
                    .comparing((ProductoOpcion o) -> o.getOrden() == null ? 0 : o.getOrden())
                    .thenComparing(o -> o.getId() == null ? Integer.MAX_VALUE : o.getId()));
            List<ProductoOpcionDTO> oDtos = new ArrayList<>();
            for (ProductoOpcion opcion : opciones) {
                if (filtrarInactivas && !Boolean.TRUE.equals(opcion.getActivo())) {
                    continue;
                }
                ProductoOpcionDTO oDto = new ProductoOpcionDTO();
                oDto.setId(opcion.getId());
                oDto.setNombre(opcion.getNombre());
                oDto.setOrden(opcion.getOrden());
                oDto.setActivo(Boolean.TRUE.equals(opcion.getActivo()));
                oDtos.add(oDto);
            }
            gDto.setOpciones(oDtos);
            result.add(gDto);
        }
        return result;
    }

    private static String nombreCompleto(Usuario usuario) {
        String nombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String apellido = usuario.getApellido() != null ? usuario.getApellido().trim() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? null : completo;
    }

    public static Producto mapProductoDTOToProducto(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setId(dto.getId());
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setImagenUrl(dto.getImagenUrl());
        producto.setIngredientes(dto.getIngredientes());
        producto.setPrecio(dto.getPrecio());
        producto.setPuntosValor(dto.getPuntosValor());
        producto.setTipo(dto.getTipo());
        producto.setEsCombo(dto.getEsCombo());
        producto.setEstadoAprobacion(dto.getEstadoAprobacion());
        producto.setActivo(dto.getActivo());
        return producto;
    }
}
