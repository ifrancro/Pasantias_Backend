package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.PedidoItemOpcion;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validación autoritativa y materialización de snapshots para selecciones de opciones en pedidos.
 */
public final class PedidoItemOpcionesSupport {

    private PedidoItemOpcionesSupport() {
    }

    public static List<PedidoItemOpcion> validarYMaterializar(
            Producto producto, List<PedidoItemOpcionResponseDTO> requestOpciones) {
        List<PedidoItemOpcionResponseDTO> selecciones = requestOpciones == null ? List.of() : requestOpciones;
        List<ProductoGrupoOpcion> grupos = gruposDelProducto(producto);
        inicializarOpciones(grupos);

        if (grupos.isEmpty()) {
            if (!selecciones.isEmpty()) {
                OrderCreationRejections.throwOptionInvalid(
                        "El producto no tiene grupos de opciones configurados");
            }
            return List.of();
        }

        Map<Integer, ProductoGrupoOpcion> gruposPorId = indexGrupos(grupos);
        Map<Integer, ProductoOpcion> opcionesPorId = indexOpciones(grupos);

        validarSeleccionesIndividuales(selecciones, gruposPorId, opcionesPorId, producto);
        validarReglasPorGrupo(grupos, selecciones, gruposPorId, opcionesPorId);

        return materializar(selecciones, gruposPorId, opcionesPorId);
    }

    private static List<ProductoGrupoOpcion> gruposDelProducto(Producto producto) {
        if (producto.getGruposOpciones() == null || producto.getGruposOpciones().isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(producto.getGruposOpciones());
    }

    private static void inicializarOpciones(List<ProductoGrupoOpcion> grupos) {
        for (ProductoGrupoOpcion grupo : grupos) {
            if (grupo.getOpciones() != null) {
                grupo.getOpciones().size();
            }
        }
    }

    private static Map<Integer, ProductoGrupoOpcion> indexGrupos(List<ProductoGrupoOpcion> grupos) {
        Map<Integer, ProductoGrupoOpcion> map = new HashMap<>();
        for (ProductoGrupoOpcion grupo : grupos) {
            if (grupo.getId() != null) {
                map.put(grupo.getId(), grupo);
            }
        }
        return map;
    }

    private static Map<Integer, ProductoOpcion> indexOpciones(List<ProductoGrupoOpcion> grupos) {
        Map<Integer, ProductoOpcion> map = new HashMap<>();
        for (ProductoGrupoOpcion grupo : grupos) {
            if (grupo.getOpciones() == null) {
                continue;
            }
            for (ProductoOpcion opcion : grupo.getOpciones()) {
                if (opcion.getId() != null) {
                    map.put(opcion.getId(), opcion);
                }
            }
        }
        return map;
    }

    private static void validarSeleccionesIndividuales(
            List<PedidoItemOpcionResponseDTO> selecciones,
            Map<Integer, ProductoGrupoOpcion> gruposPorId,
            Map<Integer, ProductoOpcion> opcionesPorId,
            Producto producto) {
        Set<Integer> opcionesVistas = new HashSet<>();

        for (PedidoItemOpcionResponseDTO sel : selecciones) {
            if (sel == null) {
                OrderCreationRejections.throwOptionInvalid("Una selección de opción no puede ser nula");
            }
            if (sel.getGrupoId() == null) {
                OrderCreationRejections.throwOptionInvalid("grupoId es obligatorio en cada selección de opción");
            }
            if (sel.getOpcionId() == null) {
                OrderCreationRejections.throwOptionInvalid("opcionId es obligatorio en cada selección de opción");
            }
            int cantidad = sel.getCantidad() == null ? 0 : sel.getCantidad();
            if (cantidad <= 0) {
                OrderCreationRejections.throwInvalidQuantity("La cantidad de cada opción debe ser mayor a 0");
            }
            if (!opcionesVistas.add(sel.getOpcionId())) {
                OrderCreationRejections.throwOptionInvalid(
                        "La opción con id " + sel.getOpcionId() + " está repetida en el pedido");
            }

            ProductoGrupoOpcion grupo = gruposPorId.get(sel.getGrupoId());
            if (grupo == null) {
                OrderCreationRejections.throwOptionInvalid("La opción no pertenece al producto");
            }

            ProductoOpcion opcion = opcionesPorId.get(sel.getOpcionId());
            if (opcion == null) {
                OrderCreationRejections.throwOptionInvalid("La opción no pertenece al producto");
            }

            Integer grupoRealId = opcion.getGrupo() != null ? opcion.getGrupo().getId() : null;
            if (grupoRealId == null || !grupoRealId.equals(sel.getGrupoId())) {
                OrderCreationRejections.throwOptionInvalid("La opción no pertenece al grupo indicado");
            }

            if (grupo.getProducto() != null && producto.getId() != null
                    && grupo.getProducto().getId() != null
                    && !producto.getId().equals(grupo.getProducto().getId())) {
                OrderCreationRejections.throwOptionInvalid("La opción no pertenece al producto");
            }

            if (!Boolean.TRUE.equals(opcion.getActivo())) {
                OrderCreationRejections.throwOptionInvalid(
                        "La opción " + opcion.getNombre() + " ya no está disponible");
            }
        }
    }

    private static void validarReglasPorGrupo(
            List<ProductoGrupoOpcion> grupos,
            List<PedidoItemOpcionResponseDTO> selecciones,
            Map<Integer, ProductoGrupoOpcion> gruposPorId,
            Map<Integer, ProductoOpcion> opcionesPorId) {
        Map<Integer, Integer> totalPorGrupo = new HashMap<>();
        for (PedidoItemOpcionResponseDTO sel : selecciones) {
            totalPorGrupo.merge(sel.getGrupoId(), sel.getCantidad(), Integer::sum);
        }

        for (ProductoGrupoOpcion grupo : grupos) {
            int min = grupo.getMinSelecciones() == null ? 0 : grupo.getMinSelecciones();
            Integer max = grupo.getMaxSelecciones();
            boolean permiteRepetir = Boolean.TRUE.equals(grupo.getPermiteRepetir());
            int total = totalPorGrupo.getOrDefault(grupo.getId(), 0);

            long activas = grupo.getOpciones() == null ? 0
                    : grupo.getOpciones().stream().filter(o -> Boolean.TRUE.equals(o.getActivo())).count();
            if (min > 0 && activas < min) {
                OrderCreationRejections.throwOptionInvalid(
                        "El producto ya no tiene opciones disponibles para completar " + grupo.getNombre());
            }

            if (total < min) {
                OrderCreationRejections.throwOptionInvalid(
                        "Debes seleccionar al menos " + min + " opciones de " + grupo.getNombre());
            }
            if (max != null && total > max) {
                OrderCreationRejections.throwOptionInvalid(
                        "Puedes seleccionar como máximo " + max + " opciones de " + grupo.getNombre());
            }

            if (!permiteRepetir) {
                for (PedidoItemOpcionResponseDTO sel : selecciones) {
                    if (!grupo.getId().equals(sel.getGrupoId())) {
                        continue;
                    }
                    if (sel.getCantidad() != null && sel.getCantidad() > 1) {
                        ProductoOpcion opcion = opcionesPorId.get(sel.getOpcionId());
                        String nombre = opcion != null ? opcion.getNombre() : String.valueOf(sel.getOpcionId());
                        OrderCreationRejections.throwOptionInvalid("La opción " + nombre + " no puede repetirse");
                    }
                }
            }
        }
    }

    private static List<PedidoItemOpcion> materializar(
            List<PedidoItemOpcionResponseDTO> selecciones,
            Map<Integer, ProductoGrupoOpcion> gruposPorId,
            Map<Integer, ProductoOpcion> opcionesPorId) {
        List<PedidoItemOpcion> resultado = new ArrayList<>(selecciones.size());
        for (PedidoItemOpcionResponseDTO sel : selecciones) {
            ProductoGrupoOpcion grupo = gruposPorId.get(sel.getGrupoId());
            ProductoOpcion opcion = opcionesPorId.get(sel.getOpcionId());

            PedidoItemOpcion entity = new PedidoItemOpcion();
            entity.setGrupo(grupo);
            entity.setOpcion(opcion);
            entity.setGrupoNombreSnapshot(grupo.getNombre());
            entity.setOpcionNombreSnapshot(opcion.getNombre());
            entity.setGrupoOrdenSnapshot(grupo.getOrden() == null ? 0 : grupo.getOrden());
            entity.setOpcionOrdenSnapshot(opcion.getOrden() == null ? 0 : opcion.getOrden());
            entity.setCantidad(sel.getCantidad());
            resultado.add(entity);
        }
        return resultado;
    }
}
