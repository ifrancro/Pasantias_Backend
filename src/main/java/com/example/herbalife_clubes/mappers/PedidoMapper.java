package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.pedido.PedidoComboComponenteResponseDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoComboResponseDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemDTO;
import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import com.example.herbalife_clubes.entities.Pedido;
import com.example.herbalife_clubes.entities.PedidoCombo;
import com.example.herbalife_clubes.entities.PedidoItem;

import com.example.herbalife_clubes.pedidos.PedidoDateTimeSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoMapper {
    public static PedidoDTO mapPedidoToPedidoDTO(Pedido pedido) {
        inicializarRelacionesPedido(pedido);

        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setMembresiaId(pedido.getMembresia() != null ? pedido.getMembresia().getId() : null);
        dto.setMembresiaNumeroSocio(pedido.getMembresia() != null ? pedido.getMembresia().getNumeroSocio() : null);
        dto.setClubId(pedido.getClub() != null ? pedido.getClub().getId() : null);
        dto.setClubNombre(pedido.getClub() != null ? pedido.getClub().getNombreClub() : null);

        List<PedidoItemDTO> itemsDTO = new ArrayList<>();
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            itemsDTO = pedido.getItems().stream()
                    .map(PedidoMapper::mapItemToDto)
                    .collect(Collectors.toList());
        }
        dto.setItems(itemsDTO);
        dto.setCombos(mapCombosPedido(pedido));
        dto.setTotal(calcularTotalPedido(pedido));

        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            PedidoItemDTO first = itemsDTO.get(0);
            dto.setProductoId(first.getProductoId());
            dto.setProductoNombre(first.getProductoNombre());
            dto.setCantidad(first.getCantidad());
            dto.setOpciones(first.getOpciones() != null ? new ArrayList<>(first.getOpciones()) : new ArrayList<>());
        } else {
            dto.setOpciones(new ArrayList<>());
        }

        dto.setHorarioDeseado(null);
        dto.setTipoConsumo(pedido.getTipoConsumo() != null ? pedido.getTipoConsumo().name() : null);
        dto.setTipoPago(pedido.getTipoPago());
        dto.setTiempoEstimadoMinutos(pedido.getTiempoEstimadoMinutos());
        dto.setObservaciones(pedido.getObservaciones());
        dto.setEstado(pedido.getEstado() != null ? pedido.getEstado().name() : null);
        dto.setFechaPedido(PedidoDateTimeSupport.toInstantUtc(pedido.getFechaPedido()));
        return dto;
    }

    /**
     * TOTAL = SUM(items sueltos) + SUM(pedido_combos.subtotal_snapshot).
     * Componentes de combo (pedido_combo_id != null) no suman dinero.
     */
    public static BigDecimal calcularTotalPedido(Pedido pedido) {
        BigDecimal total = BigDecimal.ZERO;
        if (pedido.getItems() != null) {
            for (PedidoItem item : pedido.getItems()) {
                if (item.getPedidoCombo() != null) {
                    continue;
                }
                total = total.add(subtotalItem(item));
            }
        }
        if (pedido.getPedidoCombos() != null) {
            for (PedidoCombo pedidoCombo : pedido.getPedidoCombos()) {
                if (pedidoCombo.getSubtotalSnapshot() != null) {
                    total = total.add(pedidoCombo.getSubtotalSnapshot());
                }
            }
        }
        return total;
    }

    private static BigDecimal subtotalItem(PedidoItem item) {
        if (item.getSubtotal() != null) {
            return item.getSubtotal();
        }
        if (item.getPrecioUnitario() != null && item.getCantidad() != null) {
            return item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
        }
        return BigDecimal.ZERO;
    }

    private static List<PedidoComboResponseDTO> mapCombosPedido(Pedido pedido) {
        if (pedido.getPedidoCombos() == null || pedido.getPedidoCombos().isEmpty()) {
            return new ArrayList<>();
        }

        return pedido.getPedidoCombos().stream()
                .sorted(Comparator.comparing(PedidoCombo::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(pedidoCombo -> {
                    List<PedidoItem> componentes = new ArrayList<>();
                    if (pedido.getItems() != null) {
                        for (PedidoItem item : pedido.getItems()) {
                            if (item.getPedidoCombo() == pedidoCombo) {
                                componentes.add(item);
                            }
                        }
                    }
                    return mapPedidoCombo(pedidoCombo, componentes);
                })
                .collect(Collectors.toList());
    }

    private static PedidoComboResponseDTO mapPedidoCombo(PedidoCombo pedidoCombo, List<PedidoItem> componentes) {
        List<PedidoComboComponenteResponseDTO> componentesDto = new ArrayList<>();
        if (componentes != null) {
            componentesDto = componentes.stream()
                    .sorted(Comparator.comparing(PedidoItem::getId, Comparator.nullsLast(Integer::compareTo)))
                    .map(PedidoMapper::mapComponenteCombo)
                    .collect(Collectors.toList());
        }

        return PedidoComboResponseDTO.builder()
                .pedidoComboId(pedidoCombo.getId())
                .comboId(pedidoCombo.getCombo() != null ? pedidoCombo.getCombo().getId() : null)
                .comboNombre(pedidoCombo.getComboNombreSnapshot())
                .cantidad(pedidoCombo.getCantidad())
                .precioUnitario(pedidoCombo.getPrecioUnitarioSnapshot())
                .subtotal(pedidoCombo.getSubtotalSnapshot())
                .puntosValor(pedidoCombo.getPuntosValorSnapshot())
                .items(componentesDto)
                .build();
    }

    private static PedidoComboComponenteResponseDTO mapComponenteCombo(PedidoItem item) {
        return PedidoComboComponenteResponseDTO.builder()
                .productoId(item.getProducto() != null ? item.getProducto().getId() : null)
                .productoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null)
                .cantidad(item.getCantidad())
                .opciones(mapOpcionesItem(item))
                .build();
    }

    private static PedidoItemDTO mapItemToDto(PedidoItem item) {
        PedidoItemDTO itemDTO = new PedidoItemDTO();
        itemDTO.setProductoId(item.getProducto() != null ? item.getProducto().getId() : null);
        itemDTO.setProductoNombre(item.getProducto() != null ? item.getProducto().getNombre() : null);
        itemDTO.setCantidad(item.getCantidad());
        itemDTO.setNota(item.getNota());
        itemDTO.setPrecioUnitario(item.getPrecioUnitario());
        itemDTO.setSubtotal(item.getSubtotal());
        itemDTO.setPedidoComboId(item.getPedidoCombo() != null ? item.getPedidoCombo().getId() : null);

        if (item.getPedidoCombo() != null) {
            itemDTO.setComboId(item.getPedidoCombo().getCombo() != null
                    ? item.getPedidoCombo().getCombo().getId()
                    : null);
            itemDTO.setComboNombre(item.getPedidoCombo().getComboNombreSnapshot());
        } else {
            itemDTO.setComboId(item.getCombo() != null ? item.getCombo().getId() : null);
            itemDTO.setComboNombre(item.getCombo() != null ? item.getCombo().getNombre() : null);
        }

        itemDTO.setOpciones(mapOpcionesItem(item));
        return itemDTO;
    }

    static List<PedidoItemOpcionResponseDTO> mapOpcionesItem(PedidoItem item) {
        if (item.getOpciones() == null || item.getOpciones().isEmpty()) {
            return new ArrayList<>();
        }
        return item.getOpciones().stream()
                .sorted(Comparator
                        .comparing((com.example.herbalife_clubes.entities.PedidoItemOpcion o) ->
                                o.getGrupoOrdenSnapshot() == null ? 0 : o.getGrupoOrdenSnapshot())
                        .thenComparing(o -> o.getOpcionOrdenSnapshot() == null ? 0 : o.getOpcionOrdenSnapshot())
                        .thenComparing(o -> o.getId() == null ? 0 : o.getId()))
                .map(PedidoMapper::mapOpcionEntityToDto)
                .collect(Collectors.toList());
    }

    private static PedidoItemOpcionResponseDTO mapOpcionEntityToDto(
            com.example.herbalife_clubes.entities.PedidoItemOpcion entity) {
        PedidoItemOpcionResponseDTO dto = new PedidoItemOpcionResponseDTO();
        dto.setGrupoId(entity.getGrupo() != null ? entity.getGrupo().getId() : null);
        dto.setGrupoNombre(entity.getGrupoNombreSnapshot());
        dto.setGrupoOrden(entity.getGrupoOrdenSnapshot());
        dto.setOpcionId(entity.getOpcion() != null ? entity.getOpcion().getId() : null);
        dto.setOpcionNombre(entity.getOpcionNombreSnapshot());
        dto.setOpcionOrden(entity.getOpcionOrdenSnapshot());
        dto.setCantidad(entity.getCantidad());
        return dto;
    }

    private static void inicializarRelacionesPedido(Pedido pedido) {
        if (pedido.getItems() != null) {
            for (PedidoItem item : pedido.getItems()) {
                if (item.getOpciones() != null) {
                    item.getOpciones().size();
                }
                if (item.getPedidoCombo() != null) {
                    item.getPedidoCombo().getComboNombreSnapshot();
                }
            }
        }
        if (pedido.getPedidoCombos() != null) {
            pedido.getPedidoCombos().size();
        }
    }

    public static Pedido mapPedidoDTOToPedido(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setId(dto.getId());
        pedido.setTiempoEstimadoMinutos(dto.getTiempoEstimadoMinutos());
        pedido.setObservaciones(dto.getObservaciones());
        return pedido;
    }
}
