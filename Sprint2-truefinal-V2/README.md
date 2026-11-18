# PowerPiteFit — Sprint 2 (versión 2) — Programación II, UTN Villa María

Sistema de gestión de gimnasio en Java, por consola, con persistencia en MySQL 9.7.4 vía JDBC.

## Novedades de esta versión

1. **Menús navegables:** menú principal con submenús *Socio*, *Listados* y *Precios de membresías*.
2. **El socio y el entrenador se piden por DNI**, nunca por id (el id sigue existiendo solo dentro de la base, para las claves foráneas). Lo único que todavía se pide por número es el ID de la clase.
3. **Precios de membresías editables** desde el menú; se guardan en la tabla `precio_membresia`.
4. **Tildes y eñes correctas** en consola (ver la sección "Caracteres raros").

## Contenido

| Carpeta / archivo | Qué es |
|---|---|
| `*.java` (carpeta raíz) | Todas las clases, en paquete por defecto (compatible con BlueJ y VS Code) |
| `sql/01_esquema.sql` | Crea la base `powerpitefit_db` y las 11 tablas (incluye `precio_membresia` con los precios iniciales) |
| `sql/02_datos_prueba.sql` | Datos de prueba (socio con membresía vencida, clase llena, etc.) |
| `sql/03_consultas.sql` | SELECT con JOIN: socios activos, clases con cupos, historial de pagos (por DNI), precios |
| `sql/04_migracion_precios.sql` | Solo para quien ya tiene la base de la versión anterior y quiere conservar sus datos |

## Menús

```
MENÚ PRINCIPAL
 1) Socio                    -> 1) Registrar socio
                                2) Contratar / renovar membresía (con pago)
                                3) Inscribir socio a clase
                                4) Crear rutina para un socio
 2) Registrar entrenador
 3) Crear clase grupal
 4) Registrar asistencia
 5) Listados                 -> 1) Rutinas de un socio
                                2) Socios activos
                                3) Clases con cupos
                                4) Historial de pagos de un socio
 6) Precios de membresías    -> 1) Ver precios actuales
                                2) Cambiar el precio de un plan
 0) Salir                       (en cada submenú, 0 = volver)
```

## Arquitectura (capas)

```
MenuConsola --> GimnasioService --> DAO (SQL/JDBC) --> MySQL
 (entrada/salida)   (transacciones)      ^
                         |               |
                         +--> Dominio (reglas de negocio)
                              Persona <- Socio, Entrenador
                              Membresia, ClaseGrupal, Rutina, Ejercicio, Pago, Asistencia
```

## Puesta en marcha

### 1. Base de datos (consola de MySQL 9.7.4)

Abrí el cliente (`mysql -u root -p --default-character-set=utf8mb4`) y, con barras `/` aunque estés en Windows:

```sql
DROP DATABASE IF EXISTS powerpitefit_db;
SOURCE C:/powerpitefit/sql/01_esquema.sql;
SOURCE C:/powerpitefit/sql/02_datos_prueba.sql;
SOURCE C:/powerpitefit/sql/03_consultas.sql;
```

Si ya tenías la base de la versión anterior y no querés perder datos, en lugar del `DROP` y los dos primeros
`SOURCE` ejecutá solo `04_migracion_precios.sql`.

### 2. Driver JDBC

Descargá **MySQL Connector/J** (serie 9.x, "Platform Independent") desde
<https://dev.mysql.com/downloads/connector/j/> y dejá el `.jar` en la carpeta `lib`.

* **VS Code:** en `.vscode/settings.json` poné `"java.project.referencedLibraries": ["lib/**/*.jar"]`.
* **BlueJ:** *Herramientas → Preferencias → Bibliotecas → Agregar* el `.jar` y reiniciar BlueJ.

### 3. Credenciales

`ConexionDB.java` usa por defecto usuario `root` y contraseña vacía. Poné tu contraseña en la constante `CLAVE`
o definí las variables de entorno `PPF_DB_USER` y `PPF_DB_PASS`. **No subas tu contraseña a GitHub.**

### 4. Ejecutar

**VS Code / BlueJ:** ejecutar `Main`. **Terminal:**

```
javac -encoding UTF-8 *.java
java -cp ".;lib/mysql-connector-j-9.x.x.jar" Main        # Windows
java -cp ".:lib/mysql-connector-j-9.x.x.jar" Main        # Linux / macOS
```

### 5. Probar las reglas SIN base de datos

```
java DemoSinBD
```

Imprime `[OK]` / `[FALLO]` por cada prueba (25 comprobaciones).

## Caracteres raros en consola (tildes, eñes, símbolos)

* **No uses `-Dstdout.encoding=UTF-8`** ni `vmArgs` equivalentes en `launch.json`: las consolas de Windows
  (cp850) no son UTF-8 y se ve basura. Java ya detecta solo el juego de caracteres de la consola.
* Los scripts SQL empiezan con `SET NAMES utf8mb4;` para que los datos se carguen en UTF-8 aunque el
  cliente de MySQL se haya abierto sin `--default-character-set=utf8mb4`.
* Si igual se ven mal, ejecutá `chcp 65001` en la terminal antes de correr el programa.
* Al ver datos con tildes dentro del cliente `mysql>` de Windows pueden verse raros: es solo la pantalla del
  cliente; lo que importa es lo que muestra el programa Java.

## Guion de pruebas con la base cargada

Socios de prueba (se identifican por DNI): **40111222** Ana Gómez, **41222333** Luis Díaz,
**42333444** Marta Ruiz, **43444555** Pedro Vega (membresía vencida).
Entrenadores: **30123456** Carlos Pérez (Crossfit) y **31234567** Lucía Fernández (Yoga).

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | 5 Listados → 2 Socios activos | Gómez, Díaz y Ruiz con sus tildes bien escritas; **no** Vega |
| 2 | 1 Socio → 3 Inscribir: DNI `43444555`, clase `2` | [NO PERMITIDO] Regla 1: membresía vencida |
| 3 | 4 Registrar asistencia: DNI `43444555`, clase `0` | [NO PERMITIDO] Regla 1 |
| 4 | 1 Socio → 3 Inscribir: DNI `42333444`, clase `1` (Crossfit) | [NO PERMITIDO] Regla 3: cupo máximo |
| 5 | 3 Crear clase: DNI entrenador `30123456`, día lunes, `09:30`, 45 min | [NO PERMITIDO] Regla 5: horario superpuesto |
| 6 | 3 Crear clase: DNI entrenador `30123456`, día lunes, `10:45`, 45 min | [OK] creada (empieza justo cuando termina Funcional) |
| 7 | 1 Socio → 2 Contratar: DNI `43444555`, MENSUAL, EFECTIVO | [OK] membresía y pago en una transacción |
| 8 | 1 Socio → 3 Inscribir: DNI `43444555`, clase `2` | [OK] inscripción realizada |
| 9 | 5 Listados → 4 Historial de pagos: DNI `43444555` | Dos pagos: el viejo y el recién registrado |
| 10 | 6 Precios → 2 Cambiar: MENSUAL a `16500` (confirmar con `s`) | [OK] precio actualizado; 6 → 1 muestra $16500.00 |
| 11 | 1 Socio → 2 Contratar: DNI `41222333`, MENSUAL | El pago sale por $16500.00; el pago viejo de Luis conserva $40000.00 |
| 12 | 1 Socio → 2 Contratar: DNI `abc` | [ERROR] pide el DNI de nuevo sin cortar el programa |

> Los pasos 6 a 11 modifican los datos. Para repetir la demostración desde cero ejecutá de nuevo
> `DROP DATABASE`, `01_esquema.sql` y `02_datos_prueba.sql`.
