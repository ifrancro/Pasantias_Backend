package com.example.herbalife_clubes.pedidos;

import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboRequestDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.*;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.ClubProductoRepository;
import com.example.herbalife_clubes.repositories.ComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Materializa {@link PedidoCombo} con snapshots y componentes a precio 0.
 * Dinero: {@code pedido_combos.subtotal_snapshot}; componentes son líneas de preparación.
 */
@Component
@RequiredArgsConstructor
public class PedidoComboSupport {

    public static final String MENSAJE_PRECIO_COMBO_INVALIDO =
            "El combo debe tener un precio de venta mayor a 0";

    private final ComboRepository comboRepository;
    private final ClubProductoRepository clubProductoRepository;

    public PedidoCombo materializar(Pedido pedido, PedidoComboRequestDTO request, Integer clubId) {
        validarRequestBasico(request);

        Combo combo = comboRepository.findByIdWithItems(request.getComboId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Combo no encontrado con id: " + request.getComboId()));

        validarComboParaPedido(combo, clubId);
        validarComposicion(combo, request.getComponentes());

        int cantidadCombo = request.getCantidad();
        BigDecimal precioUnitario = combo.getPrecio();
        PedidoCombo pedidoCombo = new PedidoCombo();
        pedidoCombo.setPedido(pedido);
        pedidoCombo.setCombo(combo);
        pedidoCombo.setComboNombreSnapshot(combo.getNombre());
        pedidoCombo.setCantidad(cantidadCombo);
        pedidoCombo.setPrecioUnitarioSnapshot(precioUnitario);
        pedidoCombo.setSubtotalSnapshot(precioUnitario.multiply(BigDecimal.valueOf(cantidadCombo)));
        pedidoCombo.setPuntosValorSnapshot(combo.getPuntosValor() != null ? combo.getPuntosValor() : 0);

        List<ComboItem> pendientes = new ArrayList<>(combo.getItems());
        for (PedidoComboComponenteRequestDTO componente : request.getComponentes()) {
            ComboItem comboItem = tomarComboItem(pendientes, componente.getProductoId());
            Producto producto = comboItem.getProducto();
            assertProductoPedible(producto, clubId);

            int cantidadItem = comboItem.getCantidad() * cantidadCombo;
            PedidoItem item = crearItemComponente(
                    pedido,
                    pedidoCombo,
                    combo,
                    producto,
                    cantidadItem,
                    componente.getOpciones());
            pedidoCombo.getItems().add(item);
            pedido.getItems().add(item);
        }

        if (!pendientes.isEmpty()) {
            throw new IllegalArgumentException("Faltan componentes del combo en la solicitud");
        }

        return pedidoCombo;
    }

    private static void validarRequestBasico(PedidoComboRequestDTO request) {
        if (request.getComboId() == null) {
            throw new IllegalArgumentException("comboId es requerido");
        }
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad del combo debe ser mayor a 0");
        }
        if (request.getComponentes() == null || request.getComponentes().isEmpty()) {
            throw new IllegalArgumentException("El combo debe incluir sus componentes configurados");
        }
    }

    public void validarComboParaPedido(Combo combo, Integer clubId) {
        if (combo.getClub() == null || !Objects.equals(combo.getClub().getId(), clubId)) {
            throw new IllegalArgumentException("El combo no pertenece al club del pedido");
        }
        if (!Boolean.TRUE.equals(combo.getActivo())) {
            throw new IllegalArgumentException("El combo no está activo");
        }
        if (combo.getPrecio() == null || combo.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(MENSAJE_PRECIO_COMBO_INVALIDO);
        }
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            throw new IllegalArgumentException("El combo no tiene productos configurados");
        }
        for (ComboItem comboItem : combo.getItems()) {
            assertProductoPedible(comboItem.getProducto(), clubId);
        }
    }

    static void validarComposicion(Combo combo, List<PedidoComboComponenteRequestDTO> componentes) {
        if (componentes.size() != combo.getItems().size()) {
            throw new IllegalArgumentException(
                    "La composición del combo no coincide: se esperaban "
                            + combo.getItems().size() + " componentes, se recibieron " + componentes.size());
        }

        Map<Integer, Long> esperados = new HashMap<>();
        for (ComboItem item : combo.getItems()) {
            Integer productoId = item.getProducto().getId();
            esperados.merge(productoId, 1L, Long::sum);
        }
        Map<Integer, Long> recibidos = new HashMap<>();
        for (PedidoComboComponenteRequestDTO componente : componentes) {
            if (componente.getProductoId() == null) {
                throw new IllegalArgumentException("Cada componente del combo debe incluir productoId");
            }
            recibidos.merge(componente.getProductoId(), 1L, Long::sum);
        }
        if (!esperados.equals(recibidos)) {
            throw new IllegalArgumentException(
                    "Los productos del combo no coinciden con la composición definida por el club");
        }
    }

    private static ComboItem tomarComboItem(List<ComboItem> pendientes, Integer productoId) {
        Iterator<ComboItem> it = pendientes.iterator();
        while (it.hasNext()) {
            ComboItem candidate = it.next();
            if (Objects.equals(candidate.getProducto().getId(), productoId)) {
                it.remove();
                return candidate;
            }
        }
        throw new IllegalArgumentException(
                "El producto " + productoId + " no pertenece a la composición del combo");
    }

    private void assertProductoPedible(Producto producto, Integer clubId) {
        if (producto == null) {
            throw new IllegalArgumentException("Producto del combo no encontrado");
        }
        if (!"APROBADO".equalsIgnoreCase(producto.getEstadoAprobacion())) {
            throw new IllegalArgumentException("El producto " + producto.getNombre() + " no está aprobado");
        }
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new IllegalArgumentException("El producto " + producto.getNombre() + " no está activo");
        }
        var cp = clubProductoRepository.findByClubIdAndProductoId(clubId, producto.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto " + producto.getNombre() + " no está configurado para este club"));
        if (cp.getDisponible() == null || !cp.getDisponible()) {
            throw new IllegalArgumentException(
                    "El producto " + producto.getNombre() + " no está disponible en este club");
        }
    }

    private static PedidoItem crearItemComponente(
            Pedido pedido,
            PedidoCombo pedidoCombo,
            Combo combo,
            Producto producto,
            int cantidad,
            List<PedidoItemOpcionResponseDTO> opcionesRequest) {
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setPedidoCombo(pedidoCombo);
        item.setCombo(combo);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(BigDecimal.ZERO);
        item.setSubtotal(BigDecimal.ZERO);

        List<PedidoItemOpcion> selecciones =
                PedidoItemOpcionesSupport.validarYMaterializar(producto, opcionesRequest);
        for (PedidoItemOpcion seleccion : selecciones) {
            seleccion.setPedidoItem(item);
            item.getOpciones().add(seleccion);
        }
        return item;
    }
}
