package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.producto.ProductoGrupoOpcionDTO;
import com.example.herbalife_clubes.dtos.producto.ProductoOpcionDTO;
import com.example.herbalife_clubes.entities.Producto;
import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import com.example.herbalife_clubes.entities.ProductoOpcion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Validación y sincronización por identidad de grupos/opciones.
 *
 * <p>PUT con {@code gruposOpciones == null} preserva la definición actual.
 * {@code gruposOpciones == []} elimina toda la definición.
 * Con contenido, sincroniza hasta que la BD refleje exactamente el payload final,
 * conservando IDs de filas sobrevivientes.
 *
 * <p>001d-B: pedido_item_opciones guarda snapshots; FKs nullable no destruyen historia.
 */
final class ProductoGruposOpcionesSupport {

    private static final int MAX_NOMBRE = 100;
    private static final String TEMP_PREFIX = "~s~";

    private ProductoGruposOpcionesSupport() {
    }

    static void aplicarSiPresente(Producto producto, List<ProductoGrupoOpcionDTO> gruposDto) {
        aplicarSiPresente(producto, gruposDto, null);
    }

    static void aplicarSiPresente(
            Producto producto, List<ProductoGrupoOpcionDTO> gruposDto, Runnable afterFlush) {
        if (gruposDto == null) {
            return;
        }
        validar(gruposDto);
        if (gruposDto.isEmpty()) {
            vaciar(producto);
            flush(afterFlush);
            return;
        }
        sincronizar(producto, gruposDto, afterFlush);
    }

    private static void sincronizar(
            Producto producto, List<ProductoGrupoOpcionDTO> gruposDto, Runnable afterFlush) {
        inicializarOpciones(producto);
        Map<Integer, ProductoGrupoOpcion> gruposPorId = indexGruposPorId(producto);
        Map<Integer, ProductoOpcion> opcionesPorId = indexOpcionesPorId(producto);

        validarIdsGruposEnPayload(gruposDto, gruposPorId);
        validarIdsOpcionesEnPayload(gruposDto, opcionesPorId);

        Set<ProductoGrupoOpcion> gruposReclamados = Collections.newSetFromMap(new IdentityHashMap<>());
        List<GrupoSync> syncs = new ArrayList<>();

        for (ProductoGrupoOpcionDTO gDto : gruposDto) {
            ProductoGrupoOpcion grupo = resolverGrupo(producto, gDto, gruposPorId, gruposReclamados);
            validarOpcionesPertenecenAlGrupo(gDto, grupo, opcionesPorId);
            syncs.add(new GrupoSync(grupo, gDto));
        }

        eliminarGruposOmitidos(producto, gruposReclamados);
        flush(afterFlush);

        for (GrupoSync sync : syncs) {
            sincronizarOpciones(sync.grupo, sync.dto, opcionesPorId, afterFlush);
        }

        prepararRenombres(
                syncs,
                s -> s.grupo.getNombre(),
                s -> s.dto.getNombre(),
                (s, nombre) -> s.grupo.setNombre(nombre),
                s -> s.grupo.getId(),
                afterFlush);

        for (GrupoSync sync : syncs) {
            aplicarCamposGrupo(sync.grupo, sync.dto);
            if (!producto.getGruposOpciones().contains(sync.grupo)) {
                producto.getGruposOpciones().add(sync.grupo);
            }
        }
    }

    private static void inicializarOpciones(Producto producto) {
        if (producto.getGruposOpciones() == null) {
            producto.setGruposOpciones(new ArrayList<>());
            return;
        }
        for (ProductoGrupoOpcion grupo : producto.getGruposOpciones()) {
            if (grupo.getOpciones() != null) {
                grupo.getOpciones().size();
            }
        }
    }

    private static Map<Integer, ProductoGrupoOpcion> indexGruposPorId(Producto producto) {
        Map<Integer, ProductoGrupoOpcion> map = new HashMap<>();
        if (producto.getGruposOpciones() == null) {
            return map;
        }
        for (ProductoGrupoOpcion grupo : producto.getGruposOpciones()) {
            if (grupo.getId() != null) {
                map.put(grupo.getId(), grupo);
            }
        }
        return map;
    }

    private static Map<Integer, ProductoOpcion> indexOpcionesPorId(Producto producto) {
        Map<Integer, ProductoOpcion> map = new HashMap<>();
        if (producto.getGruposOpciones() == null) {
            return map;
        }
        for (ProductoGrupoOpcion grupo : producto.getGruposOpciones()) {
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

    private static void validarIdsGruposEnPayload(
            List<ProductoGrupoOpcionDTO> gruposDto, Map<Integer, ProductoGrupoOpcion> gruposPorId) {
        Set<Integer> vistos = new HashSet<>();
        for (ProductoGrupoOpcionDTO gDto : gruposDto) {
            Integer id = gDto.getId();
            if (id == null) {
                continue;
            }
            if (!vistos.add(id)) {
                throw new IllegalArgumentException("El id de grupo " + id + " está repetido en el payload");
            }
            if (!gruposPorId.containsKey(id)) {
                throw new IllegalArgumentException("El grupo con id " + id + " no pertenece a este producto");
            }
        }
    }

    private static void validarIdsOpcionesEnPayload(
            List<ProductoGrupoOpcionDTO> gruposDto, Map<Integer, ProductoOpcion> opcionesPorId) {
        Set<Integer> vistos = new HashSet<>();
        for (ProductoGrupoOpcionDTO gDto : gruposDto) {
            for (ProductoOpcionDTO oDto : gDto.getOpciones()) {
                Integer id = oDto.getId();
                if (id == null) {
                    continue;
                }
                if (!vistos.add(id)) {
                    throw new IllegalArgumentException("El id de opción " + id + " está repetido en el payload");
                }
                if (!opcionesPorId.containsKey(id)) {
                    throw new IllegalArgumentException("La opción con id " + id + " no pertenece a este producto");
                }
            }
        }
    }

    private static void validarOpcionesPertenecenAlGrupo(
            ProductoGrupoOpcionDTO gDto,
            ProductoGrupoOpcion grupo,
            Map<Integer, ProductoOpcion> opcionesPorId) {
        for (ProductoOpcionDTO oDto : gDto.getOpciones()) {
            if (oDto.getId() == null) {
                continue;
            }
            ProductoOpcion existente = opcionesPorId.get(oDto.getId());
            Integer grupoExistenteId = existente.getGrupo() != null ? existente.getGrupo().getId() : null;
            Integer grupoDestinoId = grupo.getId();
            if (grupoExistenteId != null && grupoDestinoId != null && !grupoExistenteId.equals(grupoDestinoId)) {
                throw new IllegalArgumentException(
                        "La opción con id " + oDto.getId() + " no pertenece al grupo indicado");
            }
            if (grupoExistenteId != null && grupoDestinoId == null) {
                throw new IllegalArgumentException(
                        "La opción con id " + oDto.getId() + " no puede moverse a otro grupo");
            }
        }
    }

    private static ProductoGrupoOpcion resolverGrupo(
            Producto producto,
            ProductoGrupoOpcionDTO gDto,
            Map<Integer, ProductoGrupoOpcion> gruposPorId,
            Set<ProductoGrupoOpcion> gruposReclamados) {
        if (gDto.getId() != null) {
            ProductoGrupoOpcion grupo = gruposPorId.get(gDto.getId());
            gruposReclamados.add(grupo);
            return grupo;
        }

        String clave = claveNombre(gDto.getNombre());
        for (ProductoGrupoOpcion candidato : producto.getGruposOpciones()) {
            if (gruposReclamados.contains(candidato)) {
                continue;
            }
            if (clave.equals(claveNombre(candidato.getNombre()))) {
                gruposReclamados.add(candidato);
                return candidato;
            }
        }

        ProductoGrupoOpcion nuevo = new ProductoGrupoOpcion();
        nuevo.setProducto(producto);
        nuevo.setOpciones(new ArrayList<>());
        gruposReclamados.add(nuevo);
        return nuevo;
    }

    private static void eliminarGruposOmitidos(Producto producto, Set<ProductoGrupoOpcion> gruposReclamados) {
        producto.getGruposOpciones().removeIf(g -> !gruposReclamados.contains(g));
    }

    private static void sincronizarOpciones(
            ProductoGrupoOpcion grupo,
            ProductoGrupoOpcionDTO gDto,
            Map<Integer, ProductoOpcion> opcionesPorId,
            Runnable afterFlush) {
        if (grupo.getOpciones() == null) {
            grupo.setOpciones(new ArrayList<>());
        }

        Set<ProductoOpcion> opcionesReclamadas = Collections.newSetFromMap(new IdentityHashMap<>());
        List<OpcionSync> syncs = new ArrayList<>();

        for (ProductoOpcionDTO oDto : gDto.getOpciones()) {
            ProductoOpcion opcion = resolverOpcion(grupo, oDto, opcionesPorId, opcionesReclamadas);
            syncs.add(new OpcionSync(opcion, oDto, opcion.getId() == null));
        }

        grupo.getOpciones().removeIf(o -> !opcionesReclamadas.contains(o));
        flush(afterFlush);

        prepararRenombres(
                syncs,
                s -> s.opcion.getNombre(),
                s -> s.dto.getNombre(),
                (s, nombre) -> s.opcion.setNombre(nombre),
                s -> s.opcion.getId(),
                afterFlush);

        for (OpcionSync sync : syncs) {
            aplicarCamposOpcion(sync.opcion, sync.dto, sync.nueva);
            if (!grupo.getOpciones().contains(sync.opcion)) {
                grupo.getOpciones().add(sync.opcion);
            }
        }
    }

    private static ProductoOpcion resolverOpcion(
            ProductoGrupoOpcion grupo,
            ProductoOpcionDTO oDto,
            Map<Integer, ProductoOpcion> opcionesProductoPorId,
            Set<ProductoOpcion> opcionesReclamadas) {
        if (oDto.getId() != null) {
            ProductoOpcion opcion = opcionesProductoPorId.get(oDto.getId());
            opcionesReclamadas.add(opcion);
            return opcion;
        }

        String clave = claveNombre(oDto.getNombre());
        for (ProductoOpcion candidato : grupo.getOpciones()) {
            if (opcionesReclamadas.contains(candidato)) {
                continue;
            }
            if (clave.equals(claveNombre(candidato.getNombre()))) {
                opcionesReclamadas.add(candidato);
                return candidato;
            }
        }

        ProductoOpcion nuevo = new ProductoOpcion();
        nuevo.setGrupo(grupo);
        opcionesReclamadas.add(nuevo);
        return nuevo;
    }

    private static void aplicarCamposGrupo(ProductoGrupoOpcion grupo, ProductoGrupoOpcionDTO dto) {
        grupo.setNombre(dto.getNombre());
        grupo.setOrden(dto.getOrden() == null ? 0 : dto.getOrden());
        grupo.setMinSelecciones(dto.getMinSelecciones() == null ? 0 : dto.getMinSelecciones());
        grupo.setMaxSelecciones(dto.getMaxSelecciones());
        grupo.setPermiteRepetir(Boolean.TRUE.equals(dto.getPermiteRepetir()));
    }

    private static void aplicarCamposOpcion(ProductoOpcion opcion, ProductoOpcionDTO dto, boolean nueva) {
        opcion.setNombre(dto.getNombre());
        opcion.setOrden(dto.getOrden() == null ? 0 : dto.getOrden());
        if (nueva) {
            opcion.setActivo(true);
        }
    }

    /**
     * Evita violaciones UNIQUE al renombrar/intercambiar nombres: primero nombres temporales, flush, luego finales.
     */
    private static <T> void prepararRenombres(
            List<T> items,
            Function<T, String> leerNombreActual,
            Function<T, String> leerNombreDestino,
            java.util.function.BiConsumer<T, String> escribirNombre,
            Function<T, Integer> leerId,
            Runnable afterFlush) {
        boolean necesitaTemp = false;
        for (T item : items) {
            String destino = leerNombreDestino.apply(item);
            String actual = leerNombreActual.apply(item);
            if (destino == null || destino.equals(actual)) {
                continue;
            }
            for (T otro : items) {
                if (item == otro) {
                    continue;
                }
                if (destino.equals(leerNombreActual.apply(otro))) {
                    necesitaTemp = true;
                    break;
                }
            }
            if (necesitaTemp) {
                break;
            }
        }

        if (!necesitaTemp) {
            return;
        }

        int seq = 0;
        for (T item : items) {
            String destino = leerNombreDestino.apply(item);
            String actual = leerNombreActual.apply(item);
            if (destino == null || destino.equals(actual)) {
                continue;
            }
            Integer id = leerId.apply(item);
            escribirNombre.accept(item, tempNombre(id != null ? id : --seq));
        }
        flush(afterFlush);
    }

    private static String tempNombre(int id) {
        String temp = TEMP_PREFIX + id + "~";
        if (temp.length() > MAX_NOMBRE) {
            throw new IllegalStateException("Nombre temporal excede longitud máxima");
        }
        return temp;
    }

    private static String claveNombre(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
    }

    private static void flush(Runnable afterFlush) {
        if (afterFlush != null) {
            afterFlush.run();
        }
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
            String claveGrupo = claveNombre(nombreGrupo);
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
                String claveOpcion = claveNombre(nombreOpcion);
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

    private record GrupoSync(ProductoGrupoOpcion grupo, ProductoGrupoOpcionDTO dto) {
    }

    private record OpcionSync(ProductoOpcion opcion, ProductoOpcionDTO dto, boolean nueva) {
    }
}
