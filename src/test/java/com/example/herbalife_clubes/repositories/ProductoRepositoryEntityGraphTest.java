package com.example.herbalife_clubes.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductoRepositoryEntityGraphTest {

    private static final Set<String> REQUIRED_PATHS = Set.of(
            "hub", "clubCreador", "revisadoPor", "gruposOpciones", "gruposOpciones.opciones");

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

    private static void assertGraph(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = ProductoRepository.class.getMethod(methodName, paramTypes);
        EntityGraph graph = method.getAnnotation(EntityGraph.class);
        assertNotNull(graph, methodName + " debe declarar @EntityGraph(hub, clubCreador, revisadoPor)");
        Set<String> paths = Set.of(graph.attributePaths());
        assertTrue(paths.containsAll(REQUIRED_PATHS),
                methodName + " debe cargar hub, clubCreador, revisadoPor y gruposOpciones, no " + paths);
    }
}
