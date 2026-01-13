package com.example.herbalife_clubes.config;

import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
    }

    private void initializeRoles() {
        log.info("🔍 Verificando e inicializando roles básicos...");

        // Crear rol ADMIN si no existe
        if (rolRepository.findByNombre("ADMIN").isEmpty()) {
            Rol adminRol = new Rol();
            adminRol.setNombre("ADMIN");
            rolRepository.save(adminRol);
            log.info("✅ Rol ADMIN creado");
        } else {
            log.info("ℹ️  Rol ADMIN ya existe");
        }

        // Crear rol SOCIO si no existe
        if (rolRepository.findByNombre("SOCIO").isEmpty()) {
            Rol socioRol = new Rol();
            socioRol.setNombre("SOCIO");
            rolRepository.save(socioRol);
            log.info("✅ Rol SOCIO creado");
        } else {
            log.info("ℹ️  Rol SOCIO ya existe");
        }

        // Crear rol ANFITRION si no existe
        if (rolRepository.findByNombre("ANFITRION").isEmpty()) {
            Rol anfitrionRol = new Rol();
            anfitrionRol.setNombre("ANFITRION");
            rolRepository.save(anfitrionRol);
            log.info("✅ Rol ANFITRION creado");
        } else {
            log.info("ℹ️  Rol ANFITRION ya existe");
        }

        log.info("✨ Inicialización de roles completada");
    }
}

