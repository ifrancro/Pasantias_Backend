package com.example.herbalife_clubes.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MembresiaRepositoryEntityGraphTest {

    private static final Set<String> REQUIRED_PATHS = Set.of("usuario", "club", "nivel", "referidoPorMembresia.usuario");

    @Test
    void consultasQueAlimentanMembresiaMapperCarganRelaciones() throws Exception {
        assertGraph("findAll");
        assertGraph("findById", Integer.class);
        assertGraph("findByUsuarioId", Integer.class);
        assertGraph("findByClubId", Integer.class);
        assertGraph("findByNumeroSocio", String.class);
        assertGraph("findByReferidoPorMembresiaId", Integer.class);
    }

    private static void assertGraph(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = MembresiaRepository.class.getMethod(methodName, paramTypes);
        EntityGraph graph = method.getAnnotation(EntityGraph.class);
        assertNotNull(graph, methodName + " debe declarar @EntityGraph");
        Set<String> paths = Set.of(graph.attributePaths());
        assertTrue(paths.containsAll(REQUIRED_PATHS),
                methodName + " debe cargar relaciones completas, no " + paths);
    }
}
