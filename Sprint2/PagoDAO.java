import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/** DAO de Pago. */
public class PagoDAO {

    public void insertar(Connection con, Pago pago) throws SQLException {
        String sql = "INSERT INTO pago (socio_id, membresia_id, monto, fecha_hora, medio_pago) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pago.getSocio().getId());
            ps.setInt(2, pago.getMembresia().getId());
            ps.setBigDecimal(3, pago.getMonto());
            ps.setTimestamp(4, Timestamp.valueOf(pago.getFechaHora()));
            ps.setString(5, pago.getMedioPago().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para el pago.");
                pago.setId(keys.getInt(1));
            }
        }
    }
}
