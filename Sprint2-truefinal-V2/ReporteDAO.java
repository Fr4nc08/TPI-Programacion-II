import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Consultas de lectura con JOIN que imprimen reportes directamente por consola.
 * Son las mismas consultas que el archivo 03_consultas.sql, para que la defensa muestre
 * que el programa Java y la consola de MySQL devuelven exactamente lo mismo.
 * A los socios se los identifica siempre por DNI (nunca por el id interno).
 */
public class ReporteDAO {

    /** Socios con una membresía vigente hoy. */
    public void sociosActivos(Connection con) throws SQLException {
        String sql = "SELECT p.nombre, p.apellido, p.dni, m.tipo, m.fecha_vencimiento "
                + "FROM socio s "
                + "JOIN persona p ON p.id = s.id "
                + "JOIN membresia m ON m.socio_id = s.id "
                + "WHERE CURDATE() BETWEEN m.fecha_inicio AND m.fecha_vencimiento "
                + "ORDER BY p.apellido, p.nombre";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.println("\n=== SOCIOS ACTIVOS ===");
            System.out.printf("%-12s %-28s %-12s %-12s%n", "DNI", "SOCIO", "PLAN", "VENCE");
            int n = 0;
            while (rs.next()) {
                n++;
                System.out.printf("%-12s %-28s %-12s %-12s%n", rs.getString("dni"),
                        rs.getString("apellido") + ", " + rs.getString("nombre"),
                        rs.getString("tipo"), rs.getDate("fecha_vencimiento"));
            }
            if (n == 0) System.out.println("(no hay socios con membresía vigente)");
        }
    }

    /** Clases con su entrenador, inscriptos y cupos disponibles. */
    public void clasesConCupos(Connection con) throws SQLException {
        String sql = "SELECT c.id, c.nombre, c.dia_semana, c.hora_inicio, c.duracion_min, "
                + "       pe.apellido AS ent_apellido, pe.nombre AS ent_nombre, "
                + "       c.cupo_maximo, COUNT(i.socio_id) AS inscriptos, "
                + "       c.cupo_maximo - COUNT(i.socio_id) AS cupos_disponibles "
                + "FROM clase_grupal c "
                + "JOIN entrenador e ON e.id = c.entrenador_id "
                + "JOIN persona pe ON pe.id = e.id "
                + "LEFT JOIN inscripcion_clase i ON i.clase_id = c.id "
                + "GROUP BY c.id, c.nombre, c.dia_semana, c.hora_inicio, c.duracion_min, "
                + "         pe.apellido, pe.nombre, c.cupo_maximo "
                + "ORDER BY c.dia_semana, c.hora_inicio";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.println("\n=== CLASES GRUPALES Y CUPOS ===");
            System.out.printf("%-4s %-16s %-10s %-9s %-24s %-11s %-7s%n",
                    "ID", "CLASE", "DÍA", "HORA", "ENTRENADOR", "INSCRIPTOS", "LIBRES");
            int n = 0;
            while (rs.next()) {
                n++;
                int libres = rs.getInt("cupos_disponibles");
                System.out.printf("%-4d %-16s %-10s %-9s %-24s %-11s %-7s%n", rs.getInt("id"),
                        rs.getString("nombre"), rs.getString("dia_semana"), rs.getTime("hora_inicio"),
                        rs.getString("ent_apellido") + ", " + rs.getString("ent_nombre"),
                        rs.getInt("inscriptos") + "/" + rs.getInt("cupo_maximo"),
                        libres == 0 ? "LLENA" : String.valueOf(libres));
            }
            if (n == 0) System.out.println("(no hay clases cargadas)");
        }
    }

    /**
     * Todos los entrenadores con sus datos y las clases que dictan (con inscriptos y cupo).
     * Un solo SELECT con LEFT JOIN: un entrenador sin clases igual aparece. Se agrupa en Java
     * recorriendo las filas ordenadas por entrenador (sin lambdas ni colecciones extra).
     */
    public void entrenadores(Connection con) throws SQLException {
        String sql = "SELECT e.id, p.nombre, p.apellido, p.dni, p.telefono, e.especialidad, "
                + "       c.id AS clase_id, c.nombre AS clase, c.dia_semana, c.hora_inicio, c.duracion_min, "
                + "       c.cupo_maximo, COUNT(i.socio_id) AS inscriptos "
                + "FROM entrenador e "
                + "JOIN persona p ON p.id = e.id "
                + "LEFT JOIN clase_grupal c ON c.entrenador_id = e.id "
                + "LEFT JOIN inscripcion_clase i ON i.clase_id = c.id "
                + "GROUP BY e.id, p.nombre, p.apellido, p.dni, p.telefono, e.especialidad, "
                + "         c.id, c.nombre, c.dia_semana, c.hora_inicio, c.duracion_min, c.cupo_maximo "
                + "ORDER BY p.apellido, p.nombre, e.id, "
                + "         FIELD(c.dia_semana, 'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), "
                + "         c.hora_inicio";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.println("\n=== ENTRENADORES ===");
            int entrenadores = 0;
            int idActual = -1;
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id != idActual) {                       // cambió el entrenador: imprimir su encabezado
                    idActual = id;
                    entrenadores++;
                    String telefono = rs.getString("telefono");
                    System.out.println();
                    System.out.println(rs.getString("apellido") + ", " + rs.getString("nombre"));
                    System.out.println("   DNI: " + rs.getString("dni")
                            + "   Especialidad: " + rs.getString("especialidad")
                            + "   Teléfono: " + (telefono == null ? "(sin cargar)" : telefono));
                    if (rs.getObject("clase_id") == null) {
                        System.out.println("   Clases: (no dicta ninguna clase)");
                    } else {
                        System.out.printf("   %-4s %-16s %-10s %-13s %-11s%n",
                                "ID", "CLASE", "DÍA", "HORARIO", "INSCRIPTOS");
                    }
                }
                if (rs.getObject("clase_id") != null) {     // una línea por clase del entrenador
                    java.time.LocalTime inicio = rs.getTime("hora_inicio").toLocalTime();
                    java.time.LocalTime fin = inicio.plusMinutes(rs.getInt("duracion_min"));
                    System.out.printf("   %-4d %-16s %-10s %-13s %-11s%n", rs.getInt("clase_id"),
                            rs.getString("clase"), rs.getString("dia_semana"),
                            inicio.toString() + "-" + fin.toString(),
                            rs.getInt("inscriptos") + "/" + rs.getInt("cupo_maximo"));
                }
            }
            if (entrenadores == 0) System.out.println("(no hay entrenadores registrados)");
            else System.out.println("\nTotal de entrenadores: " + entrenadores);
        }
    }

    /** Historial de pagos de un socio (buscado por DNI), del más reciente al más antiguo. */
    public void historialPagos(Connection con, Socio socio) throws SQLException {
        String sql = "SELECT pg.id, pg.fecha_hora, pg.monto, pg.medio_pago, m.tipo, "
                + "       m.fecha_inicio, m.fecha_vencimiento "
                + "FROM pago pg "
                + "JOIN persona p ON p.id = pg.socio_id "
                + "JOIN membresia m ON m.id = pg.membresia_id "
                + "WHERE p.dni = ? "
                + "ORDER BY pg.fecha_hora DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, socio.getDni());
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n=== HISTORIAL DE PAGOS: " + socio.getApellido() + ", " + socio.getNombre()
                        + " (DNI " + socio.getDni() + ") ===");
                System.out.printf("%-20s %-14s %-15s %-11s %-23s%n",
                        "FECHA", "MONTO", "MEDIO", "PLAN", "PERÍODO");
                int n = 0;
                while (rs.next()) {
                    n++;
                    System.out.printf("%-20s $%-13s %-15s %-11s %s a %s%n",
                            rs.getTimestamp("fecha_hora").toString().substring(0, 16), rs.getBigDecimal("monto"),
                            rs.getString("medio_pago"), rs.getString("tipo"),
                            rs.getDate("fecha_inicio"), rs.getDate("fecha_vencimiento"));
                }
                if (n == 0) System.out.println("(el socio no tiene pagos registrados)");
            }
        }
    }
}
