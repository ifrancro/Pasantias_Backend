-- Script SQL para inicializar roles básicos
-- Ejecutar este script en tu base de datos PostgreSQL si prefieres hacerlo manualmente

-- Insertar roles solo si no existen
INSERT INTO roles (nombre) 
SELECT 'ADMIN' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'ADMIN');

INSERT INTO roles (nombre) 
SELECT 'SOCIO' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'SOCIO');

INSERT INTO roles (nombre) 
SELECT 'ANFITRION' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'ANFITRION');

-- Verificar los roles creados
SELECT * FROM roles ORDER BY id;

