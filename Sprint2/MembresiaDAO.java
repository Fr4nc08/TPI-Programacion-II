import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** DAO de Membresia. */
public class MembresiaDAO {

    public void insertar(Connection con, Membresia m, int socioId) throws SQLException {
        String sql = "INSERT INTO membresia (socio_id, tipo, fecha_inicio, fecha_vencimiento, precio) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, socioId);
            ps.setString(2, m.getTipo().name());
            ps.setDate(3, Date.valueOf(m.getFechaInicio()));
            ps.setDate(4, Date.valueOf(m.getFechaVencimiento()));
            ps.setBigDecimal(5, m.getPrecio());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para la membresía.");
                m.setId(keys.getInt(1));
            }
        }
    }

    public List<Membresia> listarPorSocio(Connection con, int socioId) throws SQLException {
        List<Membresia> lista = new ArrayList<>();
        String sql = "SELECT id, tipo, fecha_inicio, fecha_vencimiento, precio FROM membresia "
                + "WHERE socio_id = ? ORDER BY fecha_inicio";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, socioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Membresia(rs.getInt("id"), TipoMembresia.valueOf(rs.getString("tipo")),
                            rs.getDate("fecha_inicio").toLocalDate(),
                            rs.getDate("fecha_vencimiento").toLocalDate(),
                            rs.getBigDecimal("precio")));
                }
            }
        }
        return lista;
    }
}
