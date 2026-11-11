import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

/** DAO de Asistencia. clase_id es opcional (acceso libre a sala), por eso se usa setNull. */
public class AsistenciaDAO {

    public void insertar(Connection con, Asistencia asistencia) throws SQLException {
        String sql = "INSERT INTO asistencia (socio_id, clase_id, fecha_hora) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, asistencia.getSocio().getId());
            if (asistencia.getClase() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, asistencia.getClase().getId());
            }
            ps.setTimestamp(3, Timestamp.valueOf(asistencia.getFechaHora()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para la asistencia.");
                asistencia.setId(keys.getInt(1));
            }
        }
    }
}
