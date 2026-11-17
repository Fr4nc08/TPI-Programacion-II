import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
