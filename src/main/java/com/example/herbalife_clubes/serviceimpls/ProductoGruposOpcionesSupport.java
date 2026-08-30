package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.producto.ProductoGrupoOpcionDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoOpcionDTO;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Validación y reemplazo de la definición estructural de grupos/opciones.
 * PUT con {@code gruposOpciones != null} reemplaza la colección completa (cascade + orphanRemoval).
 * Aceptable porque aún no hay pedido_item_opciones ni club_producto_opciones.
 *
 * UNIQUE (producto_id, nombre) y UNIQUE (grupo_id, nombre): hay que vaciar, flush de DELETE
 * y recién insertar. Si no, Hibernate puede INSERT antes del DELETE y PostgreSQL rechaza
 * el mismo nombre.
 *
 * Deuda futura: cuando existan pedido_item_opciones o club_producto_opciones no se podrán
 * regenerar IDs con reemplazo ciego. Habrá que sincronizar por identidad estable.
 */
final class ProductoGruposOpcionesSupport {

    private ProductoGruposOpcionesSupport() {
    }

    static void aplicarSiPresente(Producto producto, List<ProductoGrupoOpcionDTO> gruposDto) {
        aplicarSiPresente(producto, gruposDto, null);
    }

    static void aplicarSiPresente(
            Producto producto, List<ProductoGrupoOpcionDTO> gruposDto, Runnable afterDeletes) {
        if (gruposDto == null) {
            return;
        }
        validar(gruposDto);
        vaciar(producto);
        if (afterDeletes != null) {
            afterDeletes.run();
        }
        poblar(producto, gruposDto);
    }

    static void validar(List<ProductoGrupoOpcionDTO> gruposDto) {
        if (gruposDto == null) {
            return;
        }
        Set<String> nombresGrupos = new HashSet<>();
        for (ProductoGrupoOpcionDTO grupo : gruposDto) {
            if (grupo == null) {
                throw new IllegalArgumentException("Un grupo de opciones no puede ser nulo");
            }
            String nombreGrupo = trimToNull(grupo.getNombre());
            if (nombreGrupo == null) {
                throw new IllegalArgumentException("El nombre del grupo no puede estar vacío");
            }
            grupo.setNombre(nombreGrupo);
            String claveGrupo = nombreGrupo.toLowerCase(Locale.ROOT);
            if (!nombresGrupos.add(claveGrupo)) {
                throw new IllegalArgumentException("Ya existe un grupo con el nombre '" + nombreGrupo + "'");
            }

            int orden = grupo.getOrden() == null ? 0 : grupo.getOrden();
            if (orden < 0) {
                throw new IllegalArgumentException("El orden del grupo no puede ser negativo");
            }
            grupo.setOrden(orden);

            int min = grupo.getMinSelecciones() == null ? 0 : grupo.getMinSelecciones();
            if (min < 0) {
                throw new IllegalArgumentException("minSelecciones no puede ser negativo");
            }
            grupo.setMinSelecciones(min);

            Integer max = grupo.getMaxSelecciones();
            if (max != null && max < min) {
                throw new IllegalArgumentException("maxSelecciones no puede ser menor que minSelecciones");
            }

            boolean permiteRepetir = Boolean.TRUE.equals(grupo.getPermiteRepetir());
            grupo.setPermiteRepetir(permiteRepetir);

            List<ProductoOpcionDTO> opciones = grupo.getOpciones();
            if (opciones == null || opciones.isEmpty()) {
                throw new IllegalArgumentException("El grupo '" + nombreGrupo + "' debe tener al menos una opción");
            }

            Set<String> nombresOpciones = new HashSet<>();
            int definidas = 0;
            for (ProductoOpcionDTO opcion : opciones) {
                if (opcion == null) {
                    throw new IllegalArgumentException("Una opción no puede ser nula");
                }
                String nombreOpcion = trimToNull(opcion.getNombre());
                if (nombreOpcion == null) {
                    throw new IllegalArgumentException("El nombre de la opción no puede estar vacío");
                }
                opcion.setNombre(nombreOpcion);
                String claveOpcion = nombreOpcion.toLowerCase(Locale.ROOT);
                if (!nombresOpciones.add(claveOpcion)) {
                    throw new IllegalArgumentException(
                            "La opción '" + nombreOpcion + "' está duplicada en el grupo '" + nombreGrupo + "'");
                }
                int ordenOpcion = opcion.getOrden() == null ? 0 : opcion.getOrden();
                if (ordenOpcion < 0) {
                    throw new IllegalArgumentException("El orden de la opción no puede ser negativo");
                }
                opcion.setOrden(ordenOpcion);
                definidas++;
            }

            if (!permiteRepetir && max != null && max > definidas) {
                throw new IllegalArgumentException(
                        "maxSelecciones no puede superar el número de opciones si no se permiten repeticiones");
            }
            if (min > 0) {
                int capacidad = permiteRepetir ? Integer.MAX_VALUE : definidas;
                if (min > capacidad) {
                    throw new IllegalArgumentException(
                            "minSelecciones supera las opciones disponibles del grupo '" + nombreGrupo + "'");
                }
            }
        }
    }

    static void vaciar(Producto producto) {
        if (producto.getGruposOpciones() == null) {
            producto.setGruposOpciones(new ArrayList<>());
        }
        producto.getGruposOpciones().clear();
    }

    static void poblar(Producto producto, List<ProductoGrupoOpcionDTO> gruposDto) {
        if (producto.getGruposOpciones() == null) {
            producto.setGruposOpciones(new ArrayList<>());
        }
        if (gruposDto == null || gruposDto.isEmpty()) {
            return;
        }
        for (ProductoGrupoOpcionDTO gDto : gruposDto) {
            ProductoGrupoOpcion grupo = new ProductoGrupoOpcion();
            grupo.setProducto(producto);
            grupo.setNombre(gDto.getNombre());
            grupo.setOrden(gDto.getOrden() == null ? 0 : gDto.getOrden());
            grupo.setMinSelecciones(gDto.getMinSelecciones() == null ? 0 : gDto.getMinSelecciones());
            grupo.setMaxSelecciones(gDto.getMaxSelecciones());
            grupo.setPermiteRepetir(Boolean.TRUE.equals(gDto.getPermiteRepetir()));
            grupo.setOpciones(new ArrayList<>());
            for (ProductoOpcionDTO oDto : gDto.getOpciones()) {
                ProductoOpcion opcion = new ProductoOpcion();
                opcion.setGrupo(grupo);
                opcion.setNombre(oDto.getNombre());
                opcion.setOrden(oDto.getOrden() == null ? 0 : oDto.getOrden());
                // Edición estructural del anfitrión: las opciones nacen activas.
                // No se usa DTO.activo (disponibilidad operativa es un ticket posterior).
                opcion.setActivo(true);
                grupo.getOpciones().add(opcion);
            }
            producto.getGruposOpciones().add(grupo);
        }
    }

    static boolean gruposDistintos(Producto producto, List<ProductoGrupoOpcionDTO> gruposDto) {
        return !Objects.equals(huella(producto), huellaDto(gruposDto));
    }

    private static String huella(Producto producto) {
        List<ProductoGrupoOpcion> grupos = producto.getGruposOpciones() == null
                ? List.of()
                : new ArrayList<>(producto.getGruposOpciones());
        grupos.sort(Comparator
                .comparing((ProductoGrupoOpcion g) -> g.getOrden() == null ? 0 : g.getOrden())
                .thenComparing(g -> g.getId() == null ? 0 : g.getId()));
        StringBuilder sb = new StringBuilder();
        for (ProductoGrupoOpcion g : grupos) {
            sb.append('|').append(norm(g.getNombre()))
                    .append(';').append(g.getOrden() == null ? 0 : g.getOrden())
                    .append(';').append(g.getMinSelecciones() == null ? 0 : g.getMinSelecciones())
                    .append(';').append(g.getMaxSelecciones() == null ? "" : g.getMaxSelecciones())
                    .append(';').append(Boolean.TRUE.equals(g.getPermiteRepetir()));
            List<ProductoOpcion> opciones = g.getOpciones() == null ? List.of() : new ArrayList<>(g.getOpciones());
            opciones.sort(Comparator
                    .comparing((ProductoOpcion o) -> o.getOrden() == null ? 0 : o.getOrden())
                    .thenComparing(o -> o.getId() == null ? 0 : o.getId()));
            for (ProductoOpcion o : opciones) {
                sb.append('~').append(norm(o.getNombre()))
                        .append(';').append(o.getOrden() == null ? 0 : o.getOrden());
            }
        }
        return sb.toString();
    }

    private static String huellaDto(List<ProductoGrupoOpcionDTO> gruposDto) {
        if (gruposDto == null || gruposDto.isEmpty()) {
            return "";
        }
        List<ProductoGrupoOpcionDTO> grupos = new ArrayList<>(gruposDto);
        grupos.sort(Comparator
                .comparing((ProductoGrupoOpcionDTO g) -> g.getOrden() == null ? 0 : g.getOrden())
                .thenComparing(g -> g.getId() == null ? 0 : g.getId()));
        StringBuilder sb = new StringBuilder();
        for (ProductoGrupoOpcionDTO g : grupos) {
            sb.append('|').append(norm(g.getNombre()))
                    .append(';').append(g.getOrden() == null ? 0 : g.getOrden())
                    .append(';').append(g.getMinSelecciones() == null ? 0 : g.getMinSelecciones())
                    .append(';').append(g.getMaxSelecciones() == null ? "" : g.getMaxSelecciones())
                    .append(';').append(Boolean.TRUE.equals(g.getPermiteRepetir()));
            List<ProductoOpcionDTO> opciones = g.getOpciones() == null ? List.of() : new ArrayList<>(g.getOpciones());
            opciones.sort(Comparator
                    .comparing((ProductoOpcionDTO o) -> o.getOrden() == null ? 0 : o.getOrden())
                    .thenComparing(o -> o.getId() == null ? 0 : o.getId()));
            for (ProductoOpcionDTO o : opciones) {
                sb.append('~').append(norm(o.getNombre()))
                        .append(';').append(o.getOrden() == null ? 0 : o.getOrden());
            }
        }
        return sb.toString();
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String norm(String value) {
        return value == null ? "" : value.trim();
    }
}
