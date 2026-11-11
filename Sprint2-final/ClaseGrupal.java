import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Clase grupal con día/hora fijos, un entrenador y un cupo máximo.
 * Aquí viven la Regla 1 (al inscribir) y la Regla 3 (cupo).
 * Los inscriptos se guardan en un Set: garantiza que un socio no se anote dos veces.
 */
public class ClaseGrupal {
    private int id;
    private String nombre;
    private final Entrenador entrenador;
    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private int duracionMinutos;
    private int cupoMaximo;
    private final Set<Socio> inscriptos = new LinkedHashSet<>();

    public ClaseGrupal(int id, String nombre, Entrenador entrenador, DayOfWeek diaSemana,
                       LocalTime horaInicio, int duracionMinutos, int cupoMaximo) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("La clase necesita un nombre.");
        }
        if (entrenador == null || diaSemana == null || horaInicio == null) {
            throw new IllegalArgumentException("La clase requiere entrenador, día y hora de inicio.");
        }
        if (duracionMinutos <= 0 || duracionMinutos > 240) {
            throw new IllegalArgumentException("La duración debe estar entre 1 y 240 minutos.");
        }
        if (cupoMaximo <= 0) {
            throw new IllegalArgumentException("El cupo máximo debe ser mayor a cero.");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.entrenador = entrenador;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.duracionMinutos = duracionMinutos;
        this.cupoMaximo = cupoMaximo;
    }

    // ---------- Reglas 1 y 3 ----------

    /**
     * Inscribe a un socio. Orden de validación: (1) membresía vigente, (2) no repetido, (3) cupo.
     */
    public void inscribir(Socio socio, LocalDate fecha) throws ReglaNegocioException {
        if (socio == null) throw new IllegalArgumentException("El socio no puede ser nulo.");
        socio.verificarMembresiaVigente(fecha);                       // Regla 1
        if (inscriptos.contains(socio)) {
            throw new ReglaNegocioException(socio.getNombreCompleto() + " ya está inscripto en '" + nombre + "'.");
        }
        if (inscriptos.size() >= cupoMaximo) {                        // Regla 3
            throw new CupoExcedidoException("Regla 3: la clase '" + nombre + "' alcanzó su cupo máximo ("
                    + cupoMaximo + ").");
        }
        inscriptos.add(socio);
    }

    /** Uso exclusivo de la capa de persistencia: reconstruye inscriptos leídos de la base. */
    public void cargarInscripto(Socio socio) {
        inscriptos.add(socio);
    }

    public boolean estaInscripto(Socio socio) { return inscriptos.contains(socio); }
    public int getCuposDisponibles() { return cupoMaximo - inscriptos.size(); }

    // ---------- Regla 5 (soporte) ----------

    public LocalTime getHoraFin() { return horaInicio.plusMinutes(duracionMinutos); }

    /** Dos clases se superponen si son el mismo día y sus intervalos [inicio, fin) se cruzan. */
    public boolean seSolapaCon(ClaseGrupal otra) {
        if (diaSemana != otra.diaSemana) return false;
        return horaInicio.isBefore(otra.getHoraFin()) && otra.horaInicio.isBefore(getHoraFin());
    }

    // ---------- Getters / setters ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public Entrenador getEntrenador() { return entrenador; }
    public DayOfWeek getDiaSemana() { return diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public int getCupoMaximo() { return cupoMaximo; }
    public Set<Socio> getInscriptos() { return Collections.unmodifiableSet(inscriptos); }

    @Override
    public String toString() {
        return "Clase #" + id + " '" + nombre + "' " + diaSemana + " " + horaInicio + "-" + getHoraFin()
                + " | " + entrenador.getNombreCompleto()
                + " | cupos: " + getCuposDisponibles() + "/" + cupoMaximo;
    }
}
