import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

public class PrecioMembresiaDAO {

    /** Precios de todos los planes, ordenados como están declarados en el enum. */
    public Map<TipoMembresia, BigDecimal> listar(Connection con) throws SQLException {
        Map<TipoMembresia, BigDecimal> precios = new EnumMap<>(TipoMembresia.class);
        String sql = "SELECT tipo, precio FROM precio_membresia";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                precios.put(TipoMembresia.valueOf(rs.getString("tipo")), rs.getBigDecimal("precio"));
            }
        }
        return precios;
    }

    /** Precio vigente de un plan, o null si todavía no tiene precio cargado. */
    public BigDecimal obtener(Connection con, TipoMembresia tipo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT precio FROM precio_membresia WHERE tipo = ?")) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("precio") : null;
            }
        }
    }

    /** Cambia el precio de un plan; si la fila no existía, la crea. */
    public void actualizar(Connection con, TipoMembresia tipo, BigDecimal precio) throws SQLException {
        int filas;
        try (PreparedStatement ps = con.prepareStatement("UPDATE precio_membresia SET precio = ? WHERE tipo = ?")) {
            ps.setBigDecimal(1, precio);
            ps.setString(2, tipo.name());
            filas = ps.executeUpdate();
        }
        if (filas == 0) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO precio_membresia (tipo, precio) VALUES (?, ?)")) {
                ps.setString(1, tipo.name());
                ps.setBigDecimal(2, precio);
                ps.executeUpdate();
            }
        }
    }
}
