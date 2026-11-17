import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GimnasioService {

    private final SocioDAO socioDAO = new SocioDAO();
    private final EntrenadorDAO entrenadorDAO = new EntrenadorDAO();
    private final MembresiaDAO membresiaDAO = new MembresiaDAO();
    private final PrecioMembresiaDAO precioDAO = new PrecioMembresiaDAO();
    private final ClaseGrupalDAO claseDAO = new ClaseGrupalDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final RutinaDAO rutinaDAO = new RutinaDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();


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

    // ------------------------------------------------------- precios de planes

    /** Precios vigentes de todos los planes. */
    public Map<TipoMembresia, BigDecimal> consultarPrecios() throws SQLException {
        try (Connection con = ConexionDB.getConexion()) {   // solo lectura: no hace falta transacción
            return precioDAO.listar(con);
        }
    }

  
    public void cambiarPrecio(TipoMembresia tipo, BigDecimal nuevoPrecio) throws SQLException {
        if (tipo == null) throw new IllegalArgumentException("El plan es obligatorio.");
        BigDecimal precio = Membresia.validarPrecio(nuevoPrecio);
        try (Connection con = abrirTransaccion()) {
            try {
                precioDAO.actualizar(con, tipo, precio);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------- membresía y pago

    public Pago contratarMembresia(String dni, TipoMembresia tipo, MedioPago medio)
            throws SQLException, ReglaNegocioException {
        if (tipo == null || medio == null) throw new IllegalArgumentException("Plan y medio de pago son obligatorios.");
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, dni);
                BigDecimal precio = precioDAO.obtener(con, tipo);
                if (precio == null) {
                    throw new ReglaNegocioException("El plan " + tipo + " no tiene precio cargado. "
                            + "Cargalo desde el menú Precios de membresías.");
                }
                LocalDate hoy = LocalDate.now();
                LocalDate inicio = hoy;
                Optional<Membresia> vigente = socio.getMembresiaVigente(hoy);
                if (vigente.isPresent()) {
                    inicio = vigente.get().getFechaVencimiento().plusDays(1);
                }
                Membresia membresia = Membresia.nueva(tipo, inicio, precio);
                socio.agregarMembresia(membresia);
                membresiaDAO.insertar(con, membresia, socio.getId());
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

    // ------------------------------------------------------- clases

    public ClaseGrupal crearClase(String nombre, String dniEntrenador, DayOfWeek dia, LocalTime hora,
                                  int duracionMin, int cupoMaximo) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Entrenador entrenador = exigirEntrenador(con, dniEntrenador);
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

    // ------------------------------------------- inscripción

    public ClaseGrupal inscribirSocio(String dni, int claseId) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                ClaseGrupal clase = claseDAO.buscarPorId(con, claseId, true);   // true = FOR UPDATE (evita sobreventa)
                if (clase == null) throw new ReglaNegocioException("No existe la clase #" + claseId + ".");
                Socio socio = exigirSocio(con, dni);
                LocalDate hoy = LocalDate.now();
                clase.inscribir(socio, hoy);                                    // Reglas 1 y 3
                claseDAO.insertarInscripcion(con, claseId, socio.getId(), hoy);
                con.commit();
                return clase;
            } catch (SQLException | ReglaNegocioException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ----------------------------------------------- asistencia

    /** claseId = null significa acceso libre a sala (sin clase). */
    public Asistencia registrarAsistencia(String dni, Integer claseId) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, dni);
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

    public Rutina crearRutina(String dniSocio, String dniEntrenador, String nombre, String objetivo,
                              List<Ejercicio> ejercicios) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, dniSocio);
                Entrenador entrenador = exigirEntrenador(con, dniEntrenador);
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

    public List<Rutina> rutinasDe(String dni) throws SQLException, ReglaNegocioException {
        try (Connection con = abrirTransaccion()) {
            try {
                Socio socio = exigirSocio(con, dni);
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
        try (Connection con = ConexionDB.getConexion()) { 
            reporteDAO.sociosActivos(con);
        }
    }

    public void listarClasesConCupos() throws SQLException {
        try (Connection con = ConexionDB.getConexion()) {
            reporteDAO.clasesConCupos(con);
        }
    }

    public void listarHistorialPagos(String dni) throws SQLException, ReglaNegocioException {
        try (Connection con = ConexionDB.getConexion()) {
            Socio socio = exigirSocio(con, dni);
            reporteDAO.historialPagos(con, socio);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Entrenador exigirEntrenador(Connection con, String dni) throws SQLException, ReglaNegocioException {
        String dniLimpio = Persona.normalizarDni(dni);
        Entrenador entrenador = entrenadorDAO.buscarPorDni(con, dniLimpio);
        if (entrenador == null) throw new ReglaNegocioException("No existe un entrenador con DNI " + dniLimpio + ".");
        return entrenador;
    }

    /** Busca al socio por DNI (lo valida y normaliza primero). Si no existe, lanza una regla de negocio. */
    private Socio exigirSocio(Connection con, String dni) throws SQLException, ReglaNegocioException {
        String dniLimpio = Persona.normalizarDni(dni);
        Socio socio = socioDAO.buscarPorDni(con, dniLimpio);
        if (socio == null) throw new ReglaNegocioException("No existe un socio con DNI " + dniLimpio + ".");
        return socio;
    }
}
