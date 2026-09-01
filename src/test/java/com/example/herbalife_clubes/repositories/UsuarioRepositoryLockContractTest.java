package com.example.herbalife_clubes.repositories;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryLockContractTest {

    @Test
    void findByEmailForUpdateUsaPessimisticWrite() throws NoSuchMethodException {
        Method method = UsuarioRepository.class.getMethod(
                "findByEmailForUpdate", String.class);

        Lock lock = method.getAnnotation(Lock.class);
        assertNotNull(lock, "findByEmailForUpdate debe declarar @Lock");
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query);
        assertTrue(query.value().contains("u.email = :email"));
    }
}
