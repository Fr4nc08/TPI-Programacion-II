-- =====================================================================
-- PowerPiteFit — SPRINT 2 (versión 2) — 04_migracion_precios.sql
--
-- Para quien YA TIENE la base creada con los scripts de la versión anterior y quiere conservar
-- sus datos: agrega solo la tabla nueva precio_membresia con los precios iniciales.
-- Es seguro ejecutarlo más de una vez: no pisa los precios que ya existan.
--
-- Si preferís partir de cero (recomendado si los nombres con tilde se veían rotos), NO uses este
-- script: borrá la base y ejecutá 01_esquema.sql y 02_datos_prueba.sql de nuevo.
-- =====================================================================
SET NAMES utf8mb4;

USE powerpitefit_db;

CREATE TABLE IF NOT EXISTS precio_membresia (
    tipo                  ENUM('MENSUAL', 'TRIMESTRAL', 'ANUAL') PRIMARY KEY,
    precio                DECIMAL(10,2) NOT NULL,
    ultima_actualizacion  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_precio_membresia_precio CHECK (precio > 0)
) ENGINE = InnoDB;

-- INSERT IGNORE: si el plan ya tiene precio, la fila se deja como está.
INSERT IGNORE INTO precio_membresia (tipo, precio) VALUES
    ('MENSUAL',     15000.00),
    ('TRIMESTRAL',  40000.00),
    ('ANUAL',      150000.00);

-- Verificación: deben aparecer los 3 planes.
SELECT tipo, precio, ultima_actualizacion FROM precio_membresia ORDER BY precio;
