import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Capa de servicio: orquesta DOMINIO + PERSISTENCIA.
 *  - Las reglas de negocio las aplican los objetos del dominio (Socio, ClaseGrupal, Entrenador).
 *  - El SQL lo ejecutan los DAO.
 *  - Esta clase abre la transacción, lee lo necesario, pide al dominio que valide, guarda
 *    y hace commit; ante CUALQUIER error hace rollback para no dejar datos a medias.
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

    /** Bloque de trabajo que corre dentro de una transacción. */
    private interface Operacion<T> {
        T ejecutar(Connection con) throws SQLException, ReglaNegocioException;
    }

    /** Abre conexión, desactiva el autocommit, ejecuta y confirma; si algo falla, deshace todo. */
    private <T> T enTransaccion(Operacion<T> operacion) throws SQLException, ReglaNegocioException {
        try (Connection con = ConexionDB.getConexion()) {
            con.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------ altas

    public Socio registrarSocio(String nombre, String apellido, String dni, String telefono,
                                String email, LocalDate fechaNacimiento) throws SQLException, ReglaNegocioException {
        Socio socio = new Socio(0, nombre, apellido, dni, telefono, email, fechaNacimiento, LocalDate.now());
        return enTransaccion(con -> {
            if (socioDAO.existeDni(con, socio.getDni())) {
                throw new ReglaNegocioException("Ya existe una persona registrada con el DNI " + socio.getDni() + ".");
            }
            socioDAO.insertar(con, socio);
            return socio;
        });
    }

    public Entrenador registrarEntrenador(String nombre, String apellido, String dni, String telefono,
                                          String especialidad) throws SQLException, ReglaNegocioException {
        Entrenador entrenador = new Entrenador(0, nombre, apellido, dni, telefono, especialidad);
        return enTransaccion(con -> {
            if (entrenadorDAO.existeDni(con, entrenador.getDni())) {
                throw new ReglaNegocioException("Ya existe una persona registrada con el DNI " + entrenador.getDni() + ".");
            }
            entrenadorDAO.insertar(con, entrenador);
            return entrenador;
        });
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
        return enTransaccion(con -> {
            Socio socio = exigirSocio(con, socioId);
            LocalDate hoy = LocalDate.now();
            LocalDate inicio = socio.getMembresiaVigente(hoy)
                    .map(m -> m.getFechaVencimiento().plusDays(1))
                    .orElse(hoy);
            Membresia membresia = Membresia.nueva(tipo, inicio);
            socio.agregarMembresia(membresia);
            membresiaDAO.insertar(con, membresia, socioId);
            Pago pago = new Pago(0, socio, membresia, membresia.getPrecio(), LocalDateTime.now(), medio);
            pagoDAO.insertar(con, pago);
            return pago;
        });
    }

    // ------------------------------------------------------- clases (Regla 5)

    public ClaseGrupal crearClase(String nombre, int entrenadorId, DayOfWeek dia, LocalTime hora,
                                  int duracionMin, int cupoMaximo) throws SQLException, ReglaNegocioException {
        return enTransaccion(con -> {
            Entrenador entrenador = entrenadorDAO.buscarPorId(con, entrenadorId);
            if (entrenador == null) throw new ReglaNegocioException("No existe el entrenador #" + entrenadorId + ".");
            claseDAO.cargarClasesDe(con, entrenador);                       // clases que ya dicta
            ClaseGrupal clase = new ClaseGrupal(0, nombre, entrenador, dia, hora, duracionMin, cupoMaximo);
            entrenador.asignarClase(clase);                                 // Regla 5 (lanza ConflictoHorarioException)
            claseDAO.insertar(con, clase);
            return clase;
        });
    }

    // ------------------------------------------- inscripción (Reglas 1 y 3)

    public ClaseGrupal inscribirSocio(int socioId, int claseId) throws SQLException, ReglaNegocioException {
        return enTransaccion(con -> {
            ClaseGrupal clase = claseDAO.buscarPorId(con, claseId, true);   // true = FOR UPDATE (evita sobreventa)
            if (clase == null) throw new ReglaNegocioException("No existe la clase #" + claseId + ".");
            Socio socio = exigirSocio(con, socioId);
            LocalDate hoy = LocalDate.now();
            clase.inscribir(socio, hoy);                                    // Reglas 1 y 3
            claseDAO.insertarInscripcion(con, claseId, socioId, hoy);
            return clase;
        });
    }

    // ----------------------------------------------- asistencia (Regla 1)

    /** claseId = null significa acceso libre a sala (sin clase). */
    public Asistencia registrarAsistencia(int socioId, Integer claseId) throws SQLException, ReglaNegocioException {
        return enTransaccion(con -> {
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
            return asistencia;
        });
    }

    // ---------------------------------------------------------------- rutinas

    public Rutina crearRutina(int socioId, int entrenadorId, String nombre, String objetivo,
                              List<Ejercicio> ejercicios) throws SQLException, ReglaNegocioException {
        return enTransaccion(con -> {
            Socio socio = exigirSocio(con, socioId);
            Entrenador entrenador = entrenadorDAO.buscarPorId(con, entrenadorId);
            if (entrenador == null) throw new ReglaNegocioException("No existe el entrenador #" + entrenadorId + ".");
            Rutina rutina = new Rutina(0, nombre, objetivo, socio, entrenador, LocalDate.now());
            for (Ejercicio e : ejercicios) rutina.agregarEjercicio(e);
            rutinaDAO.insertar(con, rutina);
            return rutina;
        });
    }

    public List<Rutina> rutinasDe(int socioId) throws SQLException, ReglaNegocioException {
        return enTransaccion(con -> rutinaDAO.listarPorSocio(con, exigirSocio(con, socioId)));
    }

    // --------------------------------------------------------------- reportes

    public void listarSociosActivos() throws SQLException, ReglaNegocioException {
        enTransaccion(con -> { reporteDAO.sociosActivos(con); return null; });
    }

    public void listarClasesConCupos() throws SQLException, ReglaNegocioException {
        enTransaccion(con -> { reporteDAO.clasesConCupos(con); return null; });
    }

    public void listarHistorialPagos(int socioId) throws SQLException, ReglaNegocioException {
        enTransaccion(con -> {
            exigirSocio(con, socioId);
            reporteDAO.historialPagos(con, socioId);
            return null;
        });
    }

    // ---------------------------------------------------------------- helpers

    private Socio exigirSocio(Connection con, int socioId) throws SQLException, ReglaNegocioException {
        Socio socio = socioDAO.buscarPorId(con, socioId);
        if (socio == null) throw new ReglaNegocioException("No existe el socio #" + socioId + ".");
        return socio;
    }
}
