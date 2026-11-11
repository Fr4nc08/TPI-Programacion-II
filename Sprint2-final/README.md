# PowerPiteFit — Sprint 2 (Programación II, UTN Villa María)

Sistema de gestión de gimnasio en Java, por consola, con persistencia en MySQL 9.7.4 vía JDBC.

## Contenido

| Carpeta / archivo | Qué es |
|---|---|
| `*.java` (carpeta raíz) | Todas las clases, en paquete por defecto (compatible con BlueJ) |
| `sql/01_esquema.sql` | Crea la base `powerpitefit_db` y las 10 tablas |
| `sql/02_datos_prueba.sql` | Datos de prueba (socio con membresía vencida, clase llena, etc.) |
| `sql/03_consultas.sql` | SELECT con JOIN: socios activos, clases con cupos, historial de pagos |

## Arquitectura (capas)

```
MenuConsola ──► GimnasioService ──► DAO (SQL/JDBC) ──► MySQL
 (entrada/salida)   (transacciones)      ▲
                         │               │
                         └──► Dominio (reglas de negocio)
                              Persona ◄─ Socio, Entrenador
                              Membresia, ClaseGrupal, Rutina, Ejercicio, Pago, Asistencia
```

## Puesta en marcha

### 1. Base de datos (consola de MySQL 9.7.4)

```
mysql -u root -p
```

Dentro del cliente `mysql>` (usá barras `/` aunque estés en Windows):

```sql
SOURCE C:/ruta/al/proyecto/sql/01_esquema.sql;
SOURCE C:/ruta/al/proyecto/sql/02_datos_prueba.sql;
SOURCE C:/ruta/al/proyecto/sql/03_consultas.sql;
```

### 2. Driver JDBC

Descargá **MySQL Connector/J** (serie 9.x, archivo `.jar`, opción "Platform Independent") desde
<https://dev.mysql.com/downloads/connector/j/>.

* **BlueJ:** *Herramientas → Preferencias → Bibliotecas → Agregar* el `.jar` → reiniciar BlueJ.
* **Terminal:** poné el `.jar` en el classpath (ver abajo).

### 3. Credenciales

`ConexionDB.java` usa por defecto usuario `root` y contraseña vacía. Cambialo de una de estas formas:

* editando la constante `CLAVE` en `ConexionDB.java`, o
* con variables de entorno `PPF_DB_USER` y `PPF_DB_PASS` (opcional `PPF_DB_URL`).

### 4. Ejecutar

**BlueJ:** *Compilar* → clic derecho en `Main` → `void main(String[] args)`.

**Terminal:**

```
javac -encoding UTF-8 *.java
java -cp ".:mysql-connector-j-9.x.x.jar" Main            # Linux / macOS
java -cp ".;mysql-connector-j-9.x.x.jar" Main            # Windows
```

(reemplazá `9.x.x` por la versión exacta del `.jar` que descargaste.)

### 5. Probar las reglas SIN base de datos

```
java DemoSinBD
```

o desde BlueJ: clic derecho en `DemoSinBD` → `main`. Imprime `[OK]` / `[FALLO]` por cada prueba.

## Guion de pruebas con la base cargada (menú de consola)

| # | Acción en el menú | Resultado esperado |
|---|---|---|
| 1 | Opción 9 (socios activos) | Aparecen Gómez, Díaz y Ruiz; **no** Vega |
| 2 | Opción 5: socio **6** (Pedro) → clase 2 | ✘ Regla 1: membresía vencida |
| 3 | Opción 6: socio **6**, clase 0 | ✘ Regla 1: membresía vencida |
| 4 | Opción 5: socio **5** (Marta) → clase **1** (Crossfit) | ✘ Regla 3: cupo máximo |
| 5 | Opción 4: nombre "Test", entrenador **1**, lunes, **09:30**, 45 min, cupo 5 | ✘ Regla 5: conflicto horario |
| 6 | Opción 4: entrenador 1, lunes, **10:45**, 45 min | ✔ creada (empieza justo cuando termina Funcional) |
| 7 | Opción 3: socio **6**, plan MENSUAL, EFECTIVO | ✔ membresía + pago en una transacción |
| 8 | Opción 5: socio **6** → clase 2 (ahora con membresía nueva) | ✔ inscripción realizada |
| 9 | Opción 10 y opción 11 (socio 3) | Cupos actualizados / 2 pagos de Ana |

> Los pasos 7 y 8 modifican los datos: para repetir la demostración desde cero, volvé a ejecutar
> `01_esquema.sql` (con el `DROP DATABASE` descomentado) y `02_datos_prueba.sql`.
