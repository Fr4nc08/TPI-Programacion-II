-- =====================================================================
-- PowerPiteFit — SPRINT 2 — 02_datos_prueba.sql
-- Ejecutar DESPUÉS de 01_esquema.sql, sobre una base recién creada (ids explícitos).
-- Las fechas usan CURDATE(): los datos siempre quedan "vigentes" o "vencidos" respecto de HOY.
-- =====================================================================
USE powerpitefit_db;

-- Personas: 1-2 entrenadores, 3-6 socios.
INSERT INTO persona (id, nombre, apellido, dni, telefono) VALUES
    (1, 'Carlos',  'Pérez',      '30123456', '3534001122'),
    (2, 'Lucía',   'Fernández',  '31234567', '3534112233'),
    (3, 'Ana',     'Gómez',      '40111222', '3534556677'),
    (4, 'Luis',    'Díaz',       '41222333', NULL),
    (5, 'Marta',   'Ruiz',       '42333444', '3534778899'),
    (6, 'Pedro',   'Vega',       '43444555', NULL);

INSERT INTO entrenador (id, especialidad) VALUES
    (1, 'Crossfit'),
    (2, 'Yoga');

INSERT INTO socio (id, email, fecha_nacimiento, fecha_alta) VALUES
    (3, 'ana.gomez@mail.com',   '1998-05-15', CURDATE() - INTERVAL 6 MONTH),
    (4, 'luis.diaz@mail.com',   '1995-08-02', CURDATE() - INTERVAL 6 MONTH),
    (5, 'marta.ruiz@mail.com',  '2000-01-20', CURDATE() - INTERVAL 3 MONTH),
    (6, 'pedro.vega@mail.com',  '1990-03-03', CURDATE() - INTERVAL 8 MONTH);

-- Membresías.
--   Ana  : una vencida (id 1) y una vigente (id 2)     → muestra el historial
--   Luis : trimestral vigente (id 3)
--   Marta: anual vigente (id 4)
--   Pedro: ÚNICA membresía VENCIDA (id 5)              → sirve para probar la Regla 1
INSERT INTO membresia (id, socio_id, tipo, fecha_inicio, fecha_vencimiento, precio) VALUES
    (1, 3, 'MENSUAL',    CURDATE() - INTERVAL 45 DAY, CURDATE() - INTERVAL 15 DAY,   15000.00),
    (2, 3, 'MENSUAL',    CURDATE() - INTERVAL 14 DAY, CURDATE() + INTERVAL 16 DAY,   15000.00),
    (3, 4, 'TRIMESTRAL', CURDATE() - INTERVAL 10 DAY, CURDATE() + INTERVAL 80 DAY,   40000.00),
    (4, 5, 'ANUAL',      CURDATE() - INTERVAL 30 DAY, CURDATE() + INTERVAL 335 DAY, 150000.00),
    (5, 6, 'MENSUAL',    CURDATE() - INTERVAL 60 DAY, CURDATE() - INTERVAL 30 DAY,   15000.00);

INSERT INTO pago (socio_id, membresia_id, monto, fecha_hora, medio_pago) VALUES
    (3, 1, 15000.00,  NOW() - INTERVAL 45 DAY, 'EFECTIVO'),
    (3, 2, 15000.00,  NOW() - INTERVAL 14 DAY, 'TRANSFERENCIA'),
    (4, 3, 40000.00,  NOW() - INTERVAL 10 DAY, 'TARJETA'),
    (5, 4, 150000.00, NOW() - INTERVAL 30 DAY, 'TARJETA'),
    (6, 5, 15000.00,  NOW() - INTERVAL 60 DAY, 'EFECTIVO');

-- Clases. 'Crossfit' tiene cupo 2 y quedará LLENA → sirve para probar la Regla 3.
-- Carlos dicta Crossfit lunes 09:00-10:00 y Funcional lunes 10:00-10:45 (consecutivas, sin superponerse).
-- Para probar la Regla 5 intentá crear una clase de Carlos el lunes a las 09:30.
INSERT INTO clase_grupal (id, nombre, entrenador_id, dia_semana, hora_inicio, duracion_min, cupo_maximo) VALUES
    (1, 'Crossfit',  1, 'MONDAY',  '09:00:00', 60, 2),
    (2, 'Funcional', 1, 'MONDAY',  '10:00:00', 45, 10),
    (3, 'Yoga',      2, 'TUESDAY', '18:00:00', 60, 15);

INSERT INTO inscripcion_clase (clase_id, socio_id, fecha_inscripcion) VALUES
    (1, 3, CURDATE() - INTERVAL 2 DAY),   -- Ana
    (1, 4, CURDATE() - INTERVAL 1 DAY),   -- Luis  → Crossfit queda 2/2 (llena)
    (3, 5, CURDATE());                    -- Marta en Yoga

INSERT INTO asistencia (socio_id, clase_id, fecha_hora) VALUES
    (3, 1,    NOW() - INTERVAL 1 DAY),
    (5, NULL, NOW() - INTERVAL 2 HOUR);   -- sala libre

INSERT INTO rutina (id, socio_id, entrenador_id, nombre, objetivo, fecha_creacion) VALUES
    (1, 3, 1, 'Fuerza base', 'Ganar fuerza', CURDATE() - INTERVAL 10 DAY);

INSERT INTO ejercicio (rutina_id, nombre, grupo_muscular, series, repeticiones, descanso_seg) VALUES
    (1, 'Sentadilla',  'Piernas', 4, 10, 90),
    (1, 'Press banca', 'Pecho',   4, 8,  120);

-- Verificación rápida de cantidades (esperado: 6, 2, 4, 5, 5, 3, 3, 2, 1, 2).
SELECT 'persona' AS tabla, COUNT(*) AS filas FROM persona
UNION ALL SELECT 'entrenador',        COUNT(*) FROM entrenador
UNION ALL SELECT 'socio',             COUNT(*) FROM socio
UNION ALL SELECT 'membresia',         COUNT(*) FROM membresia
UNION ALL SELECT 'pago',              COUNT(*) FROM pago
UNION ALL SELECT 'clase_grupal',      COUNT(*) FROM clase_grupal
UNION ALL SELECT 'inscripcion_clase', COUNT(*) FROM inscripcion_clase
UNION ALL SELECT 'asistencia',        COUNT(*) FROM asistencia
UNION ALL SELECT 'rutina',            COUNT(*) FROM rutina
UNION ALL SELECT 'ejercicio',         COUNT(*) FROM ejercicio;
