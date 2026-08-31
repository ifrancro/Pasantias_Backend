package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.serviceimpls.AsistenciaServiceImpl;
import com.example.herbalife_clubes.serviceimpls.ClubServiceImpl;
import com.example.herbalife_clubes.serviceimpls.MembresiaServiceImpl;
import com.example.herbalife_clubes.serviceimpls.PedidoServiceImpl;
import com.example.herbalife_clubes.serviceimpls.ProductoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceTransactionalGuardTest {

    @Test
    void todosLosMetodosPublicosQueDevuelvenDTOSonTransaccionales() {
        assertTransactional(ClubServiceImpl.class);
        assertTransactional(ProductoServiceImpl.class);
        assertTransactional(MembresiaServiceImpl.class);
        assertTransactional(PedidoServiceImpl.class);
        assertTransactional(AsistenciaServiceImpl.class);
    }

    private void assertTransactional(Class<?> serviceClass) {
        boolean classIsTransactional = serviceClass.isAnnotationPresent(Transactional.class);
        
        for (Method method : serviceClass.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                Class<?> returnType = method.getReturnType();
                if (returnType.getName().endsWith("DTO") || 
                    (returnType.equals(List.class) && method.getGenericReturnType().getTypeName().contains("DTO"))) {
                    
                    if (!classIsTransactional) {
                        Transactional methodAnnotation = method.getAnnotation(Transactional.class);
                        assertNotNull(methodAnnotation, 
                            "El método publico " + method.getName() + " en " + serviceClass.getSimpleName() + 
                            " debe tener @Transactional porque devuelve un DTO.");
                    }
                }
            }
        }
    }
}
