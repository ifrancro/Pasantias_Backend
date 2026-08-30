package com.example.herbalife_clubes.repositories;

import com.example.herbalife_clubes.entities.PedidoItem;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.BatchSize;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PedidoRepositoryFetchGuardTest {

    @Test
    void pedidoRepositoryNoJoinFetchItemsOpciones() throws Exception {
        for (Method method : PedidoRepository.class.getMethods()) {
            if (!method.getName().startsWith("find")) {
                continue;
            }
            org.springframework.data.jpa.repository.Query query =
                    method.getAnnotation(org.springframework.data.jpa.repository.Query.class);
            if (query == null) {
                continue;
            }
            String jpql = query.value().toLowerCase();
            assertFalse(jpql.contains("items.opciones") || jpql.contains("i.opciones"),
                    method.getName() + " no debe JOIN FETCH items.opciones (MultipleBagFetchException)");
        }
    }

    @Test
    void pedidoItemOpcionesLazyConBatchSize() throws Exception {
        Field field = PedidoItem.class.getDeclaredField("opciones");
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        assertNotNull(oneToMany);
        assertEquals(FetchType.LAZY, oneToMany.fetch());
        BatchSize batch = field.getAnnotation(BatchSize.class);
        assertNotNull(batch);
        assertEquals(50, batch.size());
    }
}
