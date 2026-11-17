-- =====================================================================
-- PowerPiteFit — SPRINT 2 (versión 2) — 01_esquema.sql
-- Motor: MySQL 9.7.4 (consola)   |   Ejecutar UNA vez, en este orden.
--
-- NOVEDADES respecto de la versión anterior:
--   * Tabla nueva precio_membresia: los precios de los planes ahora se guardan en la base
--     y el usuario los puede cambiar desde el menú del programa.
--   * SET NAMES utf8mb4: el script se lee siempre como UTF-8, sin importar cómo se abrió el cliente
--     de MySQL. Así las tildes y eñes (Pérez, Gómez) se guardan bien en lugar de quedar corruptas.
-- =====================================================================

SET NAMES utf8mb4;

-- Para empezar de cero (borra TODO lo anterior), descomentá la línea siguiente:
-- DROP DATABASE IF EXISTS powerpitefit_db;

CREATE DATABASE IF NOT EXISTS powerpitefit_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE powerpitefit_db;

-- ---------------------------------------------------------------------
-- Herencia Persona → Socio / Entrenador: "una tabla por clase".
-- socio.id y entrenador.id son a la vez PK y FK hacia persona.id.
-- A los socios el programa los busca por DNI (UNIQUE); el id es interno, para las claves foráneas.
-- ---------------------------------------------------------------------
CREATE TABLE persona (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(50)  NOT NULL,
    apellido  VARCHAR(50)  NOT NULL,
    dni       VARCHAR(10)  NOT NULL,
    telefono  VARCHAR(20)  NULL,
    CONSTRAINT uq_persona_dni UNIQUE (dni)
) ENGINE = InnoDB;

CREATE TABLE socio (
    id                INT PRIMARY KEY,
    email             VARCHAR(100) NOT NULL,
    fecha_nacimiento  DATE         NOT NULL,
    fecha_alta        DATE         NOT NULL,
    CONSTRAINT uq_socio_email UNIQUE (email),
    CONSTRAINT fk_socio_persona FOREIGN KEY (id) REFERENCES persona (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE entrenador (
    id            INT PRIMARY KEY,
    especialidad  VARCHAR(100) NOT NULL,
    CONSTRAINT fk_entrenador_persona FOREIGN KEY (id) REFERENCES persona (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Precios vigentes de cada plan (NUEVA). Una fila por plan; el programa la actualiza
-- cuando el usuario cambia un precio. Las membresías ya contratadas NO se modifican:
-- cada una guarda en membresia.precio lo que se cobró en ese momento.
-- Los valores iniciales son los precios base; se pueden cambiar desde el menú o con un UPDATE.
-- ---------------------------------------------------------------------
CREATE TABLE precio_membresia (
    tipo                  ENUM('MENSUAL', 'TRIMESTRAL', 'ANUAL') PRIMARY KEY,
    precio                DECIMAL(10,2) NOT NULL,
    ultima_actualizacion  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_precio_membresia_precio CHECK (precio > 0)
) ENGINE = InnoDB;

INSERT INTO precio_membresia (tipo, precio) VALUES
    ('MENSUAL',     15000.00),
    ('TRIMESTRAL',  40000.00),
    ('ANUAL',      150000.00);

-- ---------------------------------------------------------------------
-- Membresía y pagos. DECIMAL (no DOUBLE) para dinero: sin errores de redondeo.
-- ---------------------------------------------------------------------
CREATE TABLE membresia (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    socio_id           INT           NOT NULL,
    tipo               ENUM('MENSUAL', 'TRIMESTRAL', 'ANUAL') NOT NULL,
    fecha_inicio       DATE          NOT NULL,
    fecha_vencimiento  DATE          NOT NULL,
    precio             DECIMAL(10,2) NOT NULL,
    CONSTRAINT ck_membresia_fechas CHECK (fecha_vencimiento > fecha_inicio),
    CONSTRAINT ck_membresia_precio CHECK (precio >= 0),
    CONSTRAINT fk_membresia_socio FOREIGN KEY (socio_id) REFERENCES socio (id) ON DELETE CASCADE,
    INDEX idx_membresia_socio_fechas (socio_id, fecha_inicio, fecha_vencimiento)
) ENGINE = InnoDB;

CREATE TABLE pago (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    socio_id      INT           NOT NULL,
    membresia_id  INT           NOT NULL,
    monto         DECIMAL(10,2) NOT NULL,
    fecha_hora    DATETIME      NOT NULL,
    medio_pago    ENUM('EFECTIVO', 'TRANSFERENCIA', 'TARJETA') NOT NULL,
    CONSTRAINT ck_pago_monto CHECK (monto > 0),
    CONSTRAINT fk_pago_socio     FOREIGN KEY (socio_id)     REFERENCES socio (id)     ON DELETE CASCADE,
    CONSTRAINT fk_pago_membresia FOREIGN KEY (membresia_id) REFERENCES membresia (id) ON DELETE CASCADE,
    INDEX idx_pago_socio_fecha (socio_id, fecha_hora)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Clases grupales. El horario se divide en día + hora + duración para poder
-- detectar superposiciones (Regla 5). La clave UNIQUE es una red de seguridad
-- extra: impide dos clases del mismo entrenador que EMPIECEN a la misma hora,
-- incluso si alguien inserta datos saltándose la aplicación Java.
-- ---------------------------------------------------------------------
CREATE TABLE clase_grupal (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(50) NOT NULL,
    entrenador_id INT         NOT NULL,
    dia_semana    ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    hora_inicio   TIME        NOT NULL,
    duracion_min  INT         NOT NULL,
    cupo_maximo   INT         NOT NULL,
    CONSTRAINT ck_clase_duracion CHECK (duracion_min > 0 AND duracion_min <= 240),
    CONSTRAINT ck_clase_cupo     CHECK (cupo_maximo > 0),
    CONSTRAINT uq_clase_entrenador_horario UNIQUE (entrenador_id, dia_semana, hora_inicio),
    CONSTRAINT fk_clase_entrenador FOREIGN KEY (entrenador_id) REFERENCES entrenador (id) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Tabla intermedia N:M entre clase y socio. La PK compuesta impide inscribir dos veces al mismo socio.
CREATE TABLE inscripcion_clase (
    clase_id           INT  NOT NULL,
    socio_id           INT  NOT NULL,
    fecha_inscripcion  DATE NOT NULL,
    PRIMARY KEY (clase_id, socio_id),
    CONSTRAINT fk_insc_clase FOREIGN KEY (clase_id) REFERENCES clase_grupal (id) ON DELETE CASCADE,
    CONSTRAINT fk_insc_socio FOREIGN KEY (socio_id) REFERENCES socio (id)        ON DELETE CASCADE
) ENGINE = InnoDB;

-- clase_id es NULL cuando el socio entra a sala libre; si se borra la clase, se conserva la asistencia.
CREATE TABLE asistencia (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    socio_id    INT      NOT NULL,
    clase_id    INT      NULL,
    fecha_hora  DATETIME NOT NULL,
    CONSTRAINT fk_asistencia_socio FOREIGN KEY (socio_id) REFERENCES socio (id)        ON DELETE CASCADE,
    CONSTRAINT fk_asistencia_clase FOREIGN KEY (clase_id) REFERENCES clase_grupal (id) ON DELETE SET NULL,
    INDEX idx_asistencia_socio_fecha (socio_id, fecha_hora)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Rutinas y ejercicios (la rutina "posee" sus ejercicios: ON DELETE CASCADE).
-- ---------------------------------------------------------------------
CREATE TABLE rutina (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    socio_id       INT          NOT NULL,
    entrenador_id  INT          NOT NULL,
    nombre         VARCHAR(80)  NOT NULL,
    objetivo       VARCHAR(150) NULL,
    fecha_creacion DATE         NOT NULL,
    CONSTRAINT fk_rutina_socio      FOREIGN KEY (socio_id)      REFERENCES socio (id)      ON DELETE CASCADE,
    CONSTRAINT fk_rutina_entrenador FOREIGN KEY (entrenador_id) REFERENCES entrenador (id) ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE ejercicio (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    rutina_id       INT         NOT NULL,
    nombre          VARCHAR(80) NOT NULL,
    grupo_muscular  VARCHAR(50) NOT NULL,
    series          INT         NOT NULL,
    repeticiones    INT         NOT NULL,
    descanso_seg    INT         NOT NULL DEFAULT 60,
    CONSTRAINT ck_ejercicio_valores CHECK (series > 0 AND repeticiones > 0 AND descanso_seg >= 0),
    CONSTRAINT fk_ejercicio_rutina FOREIGN KEY (rutina_id) REFERENCES rutina (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Verificación: debe listar 11 tablas y mostrar los 3 precios iniciales.
SHOW TABLES;
SELECT tipo, precio FROM precio_membresia ORDER BY precio;
