import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plan de entrenamiento que un entrenador diseña para un socio.
 * Composición: los ejercicios pertenecen a la rutina y no existen sin ella.
 */
public class Rutina {
    private int id;
    private String nombre;
    private String objetivo;
    private final Socio socio;
    private final Entrenador entrenador;
    private final LocalDate fechaCreacion;
    private final List<Ejercicio> ejercicios = new ArrayList<>();

    public Rutina(int id, String nombre, String objetivo, Socio socio, Entrenador entrenador, LocalDate fechaCreacion) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("La rutina necesita un nombre.");
        }
        if (socio == null || entrenador == null || fechaCreacion == null) {
            throw new IllegalArgumentException("La rutina requiere socio, entrenador y fecha de creación.");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.objetivo = (objetivo == null || objetivo.trim().isEmpty()) ? null : objetivo.trim();
        this.socio = socio;
        this.entrenador = entrenador;
        this.fechaCreacion = fechaCreacion;
    }

    public void agregarEjercicio(Ejercicio ejercicio) {
        if (ejercicio == null) throw new IllegalArgumentException("El ejercicio no puede ser nulo.");
        ejercicios.add(ejercicio);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getObjetivo() { return objetivo; }
    public Socio getSocio() { return socio; }
    public Entrenador getEntrenador() { return entrenador; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }

    /** Vista de solo lectura: nadie de afuera puede modificar la lista interna. */
    public List<Ejercicio> getEjercicios() { return Collections.unmodifiableList(ejercicios); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Rutina '" + nombre + "'");
        if (objetivo != null) sb.append(" — objetivo: ").append(objetivo);
        sb.append(" (entrenador: ").append(entrenador.getNombreCompleto()).append(")");
        for (Ejercicio e : ejercicios) sb.append("\n    · ").append(e);
        return sb.toString();
    }
}
