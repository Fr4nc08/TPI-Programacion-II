import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Socio. La tabla persona guarda los datos comunes y socio los propios
 * (herencia mapeada como "una tabla por clase", unidas por la misma clave primaria).
 * Todos los métodos reciben la Connection: la transacción la controla el Service.
 */
public class SocioDAO {

    private static final String SELECT_BASE =
            "SELECT p.id, p.nombre, p.apellido, p.dni, p.telefono, s.email, s.fecha_nacimiento, s.fecha_alta "
            + "FROM persona p JOIN socio s ON s.id = p.id ";

    /** Inserta en persona y luego en socio; asigna al objeto el id generado. */
    public void insertar(Connection con, Socio socio) throws SQLException {
        String sqlPersona = "INSERT INTO persona (nombre, apellido, dni, telefono) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, socio.getNombre());
            ps.setString(2, socio.getApellido());
            ps.setString(3, socio.getDni());
            ps.setString(4, socio.getTelefono());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para la persona.");
                socio.setId(keys.getInt(1));
            }
        }
        String sqlSocio = "INSERT INTO socio (id, email, fecha_nacimiento, fecha_alta) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlSocio)) {
            ps.setInt(1, socio.getId());
            ps.setString(2, socio.getEmail());
            ps.setDate(3, Date.valueOf(socio.getFechaNacimiento()));
            ps.setDate(4, Date.valueOf(socio.getFechaAlta()));
            ps.executeUpdate();
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

    /** Devuelve el socio con su historial de membresías, o null si no existe. */
    public Socio buscarPorId(Connection con, int id) throws SQLException {
        Socio socio = null;
        try (PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE p.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) socio = mapear(rs);
            }
        }
        if (socio != null) {
            for (Membresia m : new MembresiaDAO().listarPorSocio(con, id)) {
                socio.agregarMembresia(m);
            }
        }
        return socio;
    }

    /** Socios inscriptos a una clase (versión liviana: sin historial de membresías). */
    public List<Socio> listarPorClase(Connection con, int claseId) throws SQLException {
        List<Socio> lista = new ArrayList<>();
        String sql = SELECT_BASE + "JOIN inscripcion_clase i ON i.socio_id = s.id WHERE i.clase_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, claseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Socio mapear(ResultSet rs) throws SQLException {
        return new Socio(
                rs.getInt("id"), rs.getString("nombre"), rs.getString("apellido"),
                rs.getString("dni"), rs.getString("telefono"), rs.getString("email"),
                rs.getDate("fecha_nacimiento").toLocalDate(), rs.getDate("fecha_alta").toLocalDate());
    }
}
