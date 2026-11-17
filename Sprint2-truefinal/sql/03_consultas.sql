-- =====================================================================
-- PowerPiteFit — SPRINT 2 (versión 2) — 03_consultas.sql
-- Consultas clave con JOIN. Son las mismas que ejecuta ReporteDAO desde Java.
-- A los socios se los identifica por DNI (como en el programa), no por el id interno.
-- =====================================================================
USE powerpitefit_db;

-- ---------------------------------------------------------------------
-- 1) SOCIOS ACTIVOS: los que tienen una membresía vigente HOY.
--    Resultado esperado con los datos de prueba: Gómez Ana, Díaz Luis, Ruiz Marta (Pedro NO aparece).
-- ---------------------------------------------------------------------
SELECT p.dni,
       CONCAT(p.apellido, ', ', p.nombre) AS socio,
       m.tipo,
       m.fecha_vencimiento
FROM socio s
JOIN persona  p ON p.id = s.id
JOIN membresia m ON m.socio_id = s.id
WHERE CURDATE() BETWEEN m.fecha_inicio AND m.fecha_vencimiento
ORDER BY p.apellido, p.nombre;

-- ---------------------------------------------------------------------
-- 2) CLASES CON CUPOS: inscriptos y lugares libres. LEFT JOIN para que una clase
--    sin inscriptos igual aparezca (con 0). Crossfit debe figurar con 2/2 → 0 libres.
-- ---------------------------------------------------------------------
SELECT c.id,
       c.nombre            AS clase,
       c.dia_semana,
       c.hora_inicio,
       CONCAT(pe.apellido, ', ', pe.nombre) AS entrenador,
       c.cupo_maximo,
       COUNT(i.socio_id)                    AS inscriptos,
       c.cupo_maximo - COUNT(i.socio_id)    AS cupos_disponibles
FROM clase_grupal c
JOIN entrenador e   ON e.id  = c.entrenador_id
JOIN persona    pe  ON pe.id = e.id
LEFT JOIN inscripcion_clase i ON i.clase_id = c.id
GROUP BY c.id, c.nombre, c.dia_semana, c.hora_inicio, pe.apellido, pe.nombre, c.cupo_maximo
ORDER BY c.dia_semana, c.hora_inicio;

-- ---------------------------------------------------------------------
-- 3) HISTORIAL DE PAGOS de un socio, buscado por DNI (cambiá el DNI de la variable).
--    Con el DNI 40111222 (Ana) deben salir 2 pagos, el más reciente primero.
-- ---------------------------------------------------------------------
SET @dni = '40111222';

SELECT pg.id                                  AS pago,
       p.dni,
       CONCAT(p.apellido, ', ', p.nombre)     AS socio,
       pg.fecha_hora,
       pg.monto,
       pg.medio_pago,
       m.tipo                                 AS plan,
       m.fecha_inicio,
       m.fecha_vencimiento
FROM pago pg
JOIN persona  p ON p.id = pg.socio_id
JOIN membresia m ON m.id = pg.membresia_id
WHERE p.dni = @dni
ORDER BY pg.fecha_hora DESC;

-- ---------------------------------------------------------------------
-- 4) PRECIOS VIGENTES de cada plan (los que cambia el usuario desde el menú del programa).
-- ---------------------------------------------------------------------
SELECT tipo, precio, ultima_actualizacion
FROM precio_membresia
ORDER BY precio;

-- ---------------------------------------------------------------------
-- EXTRAS PARA LA DEFENSA
-- ---------------------------------------------------------------------

-- 5) Regla 1: socios SIN membresía vigente (los que el sistema debe rechazar). Esperado: Pedro Vega.
SELECT p.dni, CONCAT(p.apellido, ', ', p.nombre) AS socio, MAX(m.fecha_vencimiento) AS vencio_el
FROM socio s
JOIN persona p ON p.id = s.id
LEFT JOIN membresia m ON m.socio_id = s.id
GROUP BY p.id, p.dni, p.apellido, p.nombre
HAVING SUM(CURDATE() BETWEEN m.fecha_inicio AND m.fecha_vencimiento) IS NULL
    OR SUM(CURDATE() BETWEEN m.fecha_inicio AND m.fecha_vencimiento) = 0;

-- 6) Regla 5: detecta pares de clases del MISMO entrenador, MISMO día, cuyos intervalos se cruzan.
--    Con los datos de prueba debe devolver 0 filas (Crossfit termina 10:00 y Funcional empieza 10:00).
SELECT a.id AS clase_a, a.nombre AS nombre_a, b.id AS clase_b, b.nombre AS nombre_b,
       pe.dni AS dni_entrenador, a.dia_semana
FROM clase_grupal a
JOIN clase_grupal b
  ON  a.entrenador_id = b.entrenador_id
  AND a.dia_semana    = b.dia_semana
  AND a.id < b.id
  AND a.hora_inicio < ADDTIME(b.hora_inicio, SEC_TO_TIME(b.duracion_min * 60))
  AND b.hora_inicio < ADDTIME(a.hora_inicio, SEC_TO_TIME(a.duracion_min * 60))
JOIN persona pe ON pe.id = a.entrenador_id;

-- 7) Rutina completa de un socio, buscado por DNI (usa la misma variable @dni de la consulta 3).
SELECT r.nombre AS rutina, CONCAT(pe.apellido, ', ', pe.nombre) AS entrenador,
       ej.nombre AS ejercicio, ej.grupo_muscular, ej.series, ej.repeticiones, ej.descanso_seg
FROM rutina r
JOIN persona   ps ON ps.id = r.socio_id
JOIN persona   pe ON pe.id = r.entrenador_id
JOIN ejercicio ej ON ej.rutina_id = r.id
WHERE ps.dni = @dni
ORDER BY ej.id;

-- ---------------------------------------------------------------------
-- 8) CAMBIAR UN PRECIO DESDE LA CONSOLA DE MYSQL (equivale a la opción del menú del programa).
--    Quitá los dos guiones de la línea para ejecutarla. Solo afecta a las membresías futuras.
-- ---------------------------------------------------------------------
-- UPDATE precio_membresia SET precio = 16500.00 WHERE tipo = 'MENSUAL';
