import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** DAO de ClaseGrupal y de sus inscripciones. */
public class ClaseGrupalDAO {

    public void insertar(Connection con, ClaseGrupal clase) throws SQLException {
        String sql = "INSERT INTO clase_grupal (nombre, entrenador_id, dia_semana, hora_inicio, duracion_min, cupo_maximo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, clase.getNombre());
            ps.setInt(2, clase.getEntrenador().getId());
            ps.setString(3, clase.getDiaSemana().name());
            ps.setTime(4, Time.valueOf(clase.getHoraInicio()));
            ps.setInt(5, clase.getDuracionMinutos());
            ps.setInt(6, clase.getCupoMaximo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id generado para la clase.");
                clase.setId(keys.getInt(1));
            }
        }
    }

    /**
     * Carga una clase con su entrenador y sus inscriptos.
     * Con bloquear = true usa SELECT ... FOR UPDATE: otra transacción que intente inscribir en la
     * misma clase debe esperar, así dos personas no pueden ocupar el último cupo a la vez.
     */
    public ClaseGrupal buscarPorId(Connection con, int id, boolean bloquear) throws SQLException {
        String sql = "SELECT id, nombre, entrenador_id, dia_semana, hora_inicio, duracion_min, cupo_maximo "
                + "FROM clase_grupal WHERE id = ?" + (bloquear ? " FOR UPDATE" : "");
        ClaseGrupal clase;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Entrenador entrenador = new EntrenadorDAO().buscarPorId(con, rs.getInt("entrenador_id"));
                clase = mapear(rs, entrenador);
            }
        }
        for (Socio s : new SocioDAO().listarPorClase(con, id)) {
            clase.cargarInscripto(s);
        }
        return clase;
    }

    /** Carga en el entrenador todas las clases que ya dicta (para validar la Regla 5). */
    public void cargarClasesDe(Connection con, Entrenador entrenador) throws SQLException {
        String sql = "SELECT id, nombre, entrenador_id, dia_semana, hora_inicio, duracion_min, cupo_maximo "
                + "FROM clase_grupal WHERE entrenador_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, entrenador.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entrenador.registrarClaseExistente(mapear(rs, entrenador));
                }
            }
        }
    }

    public void insertarInscripcion(Connection con, int claseId, int socioId, LocalDate fecha) throws SQLException {
        String sql = "INSERT INTO inscripcion_clase (clase_id, socio_id, fecha_inscripcion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, claseId);
            ps.setInt(2, socioId);
            ps.setDate(3, Date.valueOf(fecha));
            ps.executeUpdate();
        }
    }

    private ClaseGrupal mapear(ResultSet rs, Entrenador entrenador) throws SQLException {
        return new ClaseGrupal(rs.getInt("id"), rs.getString("nombre"), entrenador,
                DayOfWeek.valueOf(rs.getString("dia_semana")), rs.getTime("hora_inicio").toLocalTime(),
                rs.getInt("duracion_min"), rs.getInt("cupo_maximo"));
    }
}
