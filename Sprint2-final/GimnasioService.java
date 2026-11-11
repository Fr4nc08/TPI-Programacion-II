import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio: orquesta DOMINIO + PERSISTENCIA.
 *  - Las reglas de negocio las aplican los objetos del dominio (Socio, ClaseGrupal, Entrenador).
 *  - El SQL lo ejecutan los DAO.
 *  - Cada método abre una transacción, lee lo necesario, pide al dominio que valide, guarda
 *    y hace commit; ante CUALQUIER error hace rollback para no dejar datos a medias.
 *
 * Todos los métodos siguen el mismo esquema, sin lambdas ni interfaces funcionales:
 *
 *   try (Connection con = abrirTransaccion()) {      // se cierra sola al terminar
 *       try {
 *           ... trabajo ...
 *           con.commit();                            // todo salió bien: se confirma
 *           return resultado;
 *       } catch (SQLException | ReglaNegocioException | RuntimeException e) {
 *           con.rollback();                          // algo falló: se deshace todo
 *           throw e;
 *       }
 *   }
 */
public class GimnasioService {

    private final SocioDAO socioDAO = new SocioDAO();
    private final EntrenadorDAO entrenadorDAO = new EntrenadorDAO();
    private final MembresiaDAO membresiaDAO = new MembresiaDAO();
    private final ClaseGrupalDAO claseDAO = new ClaseGrupalDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final RutinaDAO rutinaDAO = new RutinaDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();

    /** Abre una conexión con el autocommit desactivado: todo lo que se haga queda en UNA transacción. */
    private Connection abrirTransaccion() throws SQLException {
        Connection con = ConexionDB.getConexion();
        try {
            con.setAutoCommit(false);
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }

    // ------------------------------------------------------------------ altas

    public Socio registrarSocio(String nombre, String apellido, String dni, String telefono,
                                String email, LocalDate fechaNacimiento) throws SQLException, ReglaNegocioException {
        Socio socio = new Socio(0, nombre, apellido, dni, telefono, email, fechaNacimiento, LocalDate.now());
        try (Connection con = abrirTransaccion()) {
            try {
                if (socioDAO.existeDni(con, socio.getDni())) {
                    throw new ReglaNegocioException("Ya existe una persona registrada con el DNI " + socio.getDni() + ".");
                }
                socioDAO.insertar(con, socio);
                con.commit();
                return socio;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public Entrenador registrarEntrenador(String nombre, String apellido, String dni, String telefono,
                                          String especialidad) throws SQLException, ReglaNegocioException {
        Entrenador entrenador = new Entrenador(0, nombre, apellido, dni, telefono, especialidad);
        try (Connection con = abrirTransaccion()) {
            try {
                if (entrenadorDAO.existeDni(con, entrenador.getDni())) {
                    throw new ReglaNegocioException("Ya existe una persona registrada con el DNI " + entrenador.getDni() + ".");
                }
                entrenadorDAO.insertar(con, entrenador);
                con.commit();
                return entrenador;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------- membresía y pago

    /**
     * Contrata o renueva una membresía y registra su pago en UNA sola transacción:
     * no puede quedar una membresía sin pago ni un pago sin membresía.
     * Si el socio aún tiene una membresía vigente, la nueva empieza al día siguiente de su vencimiento.
     */
    public Pago contratarMembresia(int socioId, TipoMembresia tipo, MedioPago medio)
            throws SQLException, ReglaNegocioException {
        if (tipo == null || medio == null) throw new IllegalArgumentException("Plan y medio de pago son obligatorios.");
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, socioId);
                LocalDate hoy = LocalDate.now();
                LocalDate inicio = hoy;
                Optional<Membresia> vigente = socio.getMembresiaVigente(hoy);
                if (vigente.isPresent()) {
                    inicio = vigente.get().getFechaVencimiento().plusDays(1);
                }
                Membresia membresia = Membresia.nueva(tipo, inicio);
                socio.agregarMembresia(membresia);
                membresiaDAO.insertar(con, membresia, socioId);
                Pago pago = new Pago(0, socio, membresia, membresia.getPrecio(), LocalDateTime.now(), medio);
                pagoDAO.insertar(con, pago);
                con.commit();
                return pago;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------- clases (Regla 5)

    public ClaseGrupal crearClase(String nombre, int entrenadorId, DayOfWeek dia, LocalTime hora,
                                  int duracionMin, int cupoMaximo) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Entrenador entrenador = entrenadorDAO.buscarPorId(con, entrenadorId);
                if (entrenador == null) throw new ReglaNegocioException("No existe el entrenador #" + entrenadorId + ".");
                claseDAO.cargarClasesDe(con, entrenador);                       // clases que ya dicta
                ClaseGrupal clase = new ClaseGrupal(0, nombre, entrenador, dia, hora, duracionMin, cupoMaximo);
                entrenador.asignarClase(clase);                                 // Regla 5 (lanza ConflictoHorarioException)
                claseDAO.insertar(con, clase);
                con.commit();
                return clase;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------- inscripción (Reglas 1 y 3)

    public ClaseGrupal inscribirSocio(int socioId, int claseId) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                ClaseGrupal clase = claseDAO.buscarPorId(con, claseId, true);   // true = FOR UPDATE (evita sobreventa)
                if (clase == null) throw new ReglaNegocioException("No existe la clase #" + claseId + ".");
                Socio socio = exigirSocio(con, socioId);
                LocalDate hoy = LocalDate.now();
                clase.inscribir(socio, hoy);                                    // Reglas 1 y 3
                claseDAO.insertarInscripcion(con, claseId, socioId, hoy);
                con.commit();
                return clase;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ----------------------------------------------- asistencia (Regla 1)

    /** claseId = null significa acceso libre a sala (sin clase). */
    public Asistencia registrarAsistencia(int socioId, Integer claseId) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, socioId);
                ClaseGrupal clase = null;
                if (claseId != null) {
                    clase = claseDAO.buscarPorId(con, claseId, false);
                    if (clase == null) throw new ReglaNegocioException("No existe la clase #" + claseId + ".");
                    if (!clase.estaInscripto(socio)) {
                        throw new ReglaNegocioException(socio.getNombreCompleto()
                                + " no está inscripto en la clase '" + clase.getNombre() + "'.");
                    }
                }
                Asistencia asistencia = socio.registrarAsistencia(LocalDateTime.now(), clase);   // Regla 1
                asistenciaDAO.insertar(con, asistencia);
                con.commit();
                return asistencia;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------- rutinas

    public Rutina crearRutina(int socioId, int entrenadorId, String nombre, String objetivo,
                              List<Ejercicio> ejercicios) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, socioId);
                Entrenador entrenador = entrenadorDAO.buscarPorId(con, entrenadorId);
                if (entrenador == null) throw new ReglaNegocioException("No existe el entrenador #" + entrenadorId + ".");
                Rutina rutina = new Rutina(0, nombre, objetivo, socio, entrenador, LocalDate.now());
                for (Ejercicio e : ejercicios) {
                    rutina.agregarEjercicio(e);
                }
                rutinaDAO.insertar(con, rutina);
                con.commit();
                return rutina;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<Rutina> rutinasDe(int socioId) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, socioId);
                List<Rutina> rutinas = rutinaDAO.listarPorSocio(con, socio);
                con.commit();
                return rutinas;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // --------------------------------------------------------------- reportes

    public void listarSociosActivos() throws SQLException {
        try (Connection con = ConexionDB.getConexion()) {   // solo lectura: no hace falta transacción
            reporteDAO.sociosActivos(con);
        }
    }

    public void listarClasesConCupos() throws SQLException {
        try (Connection con = ConexionDB.getConexion()) {
            reporteDAO.clasesConCupos(con);
        }
    }

    public void listarHistorialPagos(int socioId) throws SQLException, ReglaNegocioException {
        try (Connection con = ConexionDB.getConexion()) {
            exigirSocio(con, socioId);
            reporteDAO.historialPagos(con, socioId);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Socio exigirSocio(Connection con, int socioId) throws SQLException, ReglaNegocioException {
        Socio socio = socioDAO.buscarPorId(con, socioId);
        if (socio == null) throw new ReglaNegocioException("No existe el socio #" + socioId + ".");
        return socio;
    }
}
