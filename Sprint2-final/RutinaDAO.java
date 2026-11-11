import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** DAO de Rutina y sus Ejercicios (la rutina "posee" a sus ejercicios: se guardan juntos). */
public class RutinaDAO {

    public void insertar(Connection con, Rutina rutina) throws SQLException {
        String sql = "INSERT INTO rutina (socio_id, entrenador_id, nombre, objetivo, fecha_creacion) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rutina.getSocio().getId());
            ps.setInt(2, rutina.getEntrenador().getId());
            ps.setString(3, rutina.getNombre());
            ps.setString(4, rutina.getObjetivo());
            ps.setDate(5, Date.valueOf(rutina.getFechaCreacion()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para la rutina.");
                rutina.setId(keys.getInt(1));
            }
        }
        String sqlEj = "INSERT INTO ejercicio (rutina_id, nombre, grupo_muscular, series, repeticiones, descanso_seg) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlEj)) {
            for (Ejercicio e : rutina.getEjercicios()) {
                ps.setInt(1, rutina.getId());
                ps.setString(2, e.getNombre());
                ps.setString(3, e.getGrupoMuscular());
                ps.setInt(4, e.getSeries());
                ps.setInt(5, e.getRepeticiones());
                ps.setInt(6, e.getDescansoSegundos());
                ps.addBatch();   // se envían todos juntos: una sola ida al servidor
            }
            ps.executeBatch();
        }
    }

    public List<Rutina> listarPorSocio(Connection con, Socio socio) throws SQLException {
        List<Rutina> rutinas = new ArrayList<>();
        EntrenadorDAO entrenadorDAO = new EntrenadorDAO();
        String sql = "SELECT id, entrenador_id, nombre, objetivo, fecha_creacion FROM rutina "
                + "WHERE socio_id = ? ORDER BY fecha_creacion DESC, id DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, socio.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Entrenador entrenador = entrenadorDAO.buscarPorId(con, rs.getInt("entrenador_id"));
                    rutinas.add(new Rutina(rs.getInt("id"), rs.getString("nombre"), rs.getString("objetivo"),
                            socio, entrenador, rs.getDate("fecha_creacion").toLocalDate()));
                }
            }
        }
        for (Rutina r : rutinas) {
            cargarEjercicios(con, r);
        }
        return rutinas;
    }

    private void cargarEjercicios(Connection con, Rutina rutina) throws SQLException {
        String sql = "SELECT id, nombre, grupo_muscular, series, repeticiones, descanso_seg "
                + "FROM ejercicio WHERE rutina_id = ? ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rutina.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rutina.agregarEjercicio(new Ejercicio(rs.getInt("id"), rs.getString("nombre"),
                            rs.getString("grupo_muscular"), rs.getInt("series"), rs.getInt("repeticiones"),
                            rs.getInt("descanso_seg")));
                }
            }
        }
    }
}
