import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** DAO de Entrenador (tablas persona + entrenador). */
public class EntrenadorDAO {

    public void insertar(Connection con, Entrenador entrenador) throws SQLException {
        String sqlPersona = "INSERT INTO persona (nombre, apellido, dni, telefono) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entrenador.getNombre());
            ps.setString(2, entrenador.getApellido());
            ps.setString(3, entrenador.getDni());
            ps.setString(4, entrenador.getTelefono());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para la persona.");
                entrenador.setId(keys.getInt(1));
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO entrenador (id, especialidad) VALUES (?, ?)")) {
            ps.setInt(1, entrenador.getId());
            ps.setString(2, entrenador.getEspecialidad());
            ps.executeUpdate();
        }
    }

    /** Devuelve el entrenador (sin sus clases cargadas) o null si no existe. */
    public Entrenador buscarPorId(Connection con, int id) throws SQLException {
        String sql = "SELECT p.id, p.nombre, p.apellido, p.dni, p.telefono, e.especialidad "
                + "FROM persona p JOIN entrenador e ON e.id = p.id WHERE p.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Entrenador(rs.getInt("id"), rs.getString("nombre"), rs.getString("apellido"),
                        rs.getString("dni"), rs.getString("telefono"), rs.getString("especialidad"));
            }
        }
    }

    public boolean existeDni(Connection con, String dni) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM persona WHERE dni = ?")) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
