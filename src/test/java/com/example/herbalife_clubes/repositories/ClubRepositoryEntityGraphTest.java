package com.example.herbalife_clubes.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClubRepositoryEntityGraphTest {

    private static final Set<String> REQUIRED_PATHS = Set.of("hub", "anfitrion");

    @Test
    void consultasQueAlimentanClubMapperCarganHubYAnfitrion() throws Exception {
        assertGraph("findAll");
        assertGraph("findById", Integer.class);
        assertGraph("findByHubId", Integer.class);
        assertGraph("findByAnfitrionId", Integer.class);
        assertGraph("findByEstadoIn", List.class);
        assertGraph("findByIdAndEstadoIn", Integer.class, List.class);
    }

    @Test
    void entityGraphNoIncluyeColeccionesDeClub() throws Exception {
        Method findAll = ClubRepository.class.getMethod("findAll");
        EntityGraph graph = findAll.getAnnotation(EntityGraph.class);
        assertNotNull(graph);
        List<String> paths = Arrays.asList(graph.attributePaths());
        assertFalse(paths.contains("fotos"));
        assertFalse(paths.contains("membresias"));
    }

    private static void assertGraph(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = ClubRepository.class.getMethod(methodName, paramTypes);
        EntityGraph graph = method.getAnnotation(EntityGraph.class);
        assertNotNull(graph, methodName + " debe declarar @EntityGraph(hub, anfitrion)");
        Set<String> paths = Set.of(graph.attributePaths());
        assertTrue(paths.containsAll(REQUIRED_PATHS),
                methodName + " debe cargar hub y anfitrion, no " + paths);
        assertFalse(paths.contains("fotos"));
        assertFalse(paths.contains("membresias"));
    }
}
