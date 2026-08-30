package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.ProductoGrupoOpcion;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductoRepositoryEntityGraphTest {

    private static final Set<String> REQUIRED_PATHS = Set.of(
            "hub", "clubCreador", "revisadoPor", "gruposOpciones");

    @Test
    void consultasQueAlimentanProductoMapperCarganHubYClubCreador() throws Exception {
        assertGraph("findAll");
        assertGraph("findById", Integer.class);
        assertGraph("findByHubId", Integer.class);
        assertGraph("findByHubIdAndActivoTrue", Integer.class);
        assertGraph("findByClubCreadorId", Integer.class);
        assertGraph("findByEstadoAprobacion", String.class);
        assertGraph("findByEstadoAprobacionNot", String.class);
        assertGraph("findByEstadoAprobacionAndClubCreadorId", String.class, Integer.class);
        assertGraph("findByHubIdAndTipoAndEstadoAprobacion", Integer.class, String.class, String.class);
        assertGraph("findByClubCreadorIdAndTipoAndEstadoAprobacion", Integer.class, String.class, String.class);
    }

    @Test
    void opcionesSeCarganLazyConBatchSizeNoPorDobleBagFetch() throws Exception {
        Field field = ProductoGrupoOpcion.class.getDeclaredField("opciones");
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        assertNotNull(oneToMany);
        assertEquals(FetchType.LAZY, oneToMany.fetch());

        BatchSize batch = field.getAnnotation(BatchSize.class);
        assertNotNull(batch, "opciones debe declarar @BatchSize para evitar N+1 descontrolado");
        assertEquals(50, batch.size());

        assertNull(field.getAnnotation(Fetch.class),
                "opciones no debe ir en el mismo JOIN FETCH que gruposOpciones (MultipleBagFetchException)");
    }

    private static void assertGraph(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = ProductoRepository.class.getMethod(methodName, paramTypes);
        EntityGraph graph = method.getAnnotation(EntityGraph.class);
        assertNotNull(graph, methodName + " debe declarar @EntityGraph(hub, clubCreador, revisadoPor, gruposOpciones)");
        Set<String> paths = Set.of(graph.attributePaths());
        assertTrue(paths.containsAll(REQUIRED_PATHS),
                methodName + " debe cargar hub, clubCreador, revisadoPor y gruposOpciones, no " + paths);
        assertFalse(paths.contains("gruposOpciones.opciones"),
                methodName + " no debe JOIN FETCH dos bags List (MultipleBagFetchException)");
    }
}
