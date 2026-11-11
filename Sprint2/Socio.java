import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Cliente del gimnasio. Hereda de Persona y agrega email, fechas y su historial de membresías.
 * Aquí vive la Regla 1: es el propio socio quien sabe si su membresía está vigente.
 */
public class Socio extends Persona {
    private String email;
    private LocalDate fechaNacimiento;
    private LocalDate fechaAlta;
    private final List<Membresia> membresias = new ArrayList<>();

    public Socio(int id, String nombre, String apellido, String dni, String telefono,
                 String email, LocalDate fechaNacimiento, LocalDate fechaAlta) {
        super(id, nombre, apellido, dni, telefono);
        setEmail(email);
        setFechaNacimiento(fechaNacimiento);
        if (fechaAlta == null) throw new IllegalArgumentException("La fecha de alta no puede ser nula.");
        this.fechaAlta = fechaAlta;
    }

    @Override
    public String getRol() { return "SOCIO"; }

    /** Override: agrega el email a la descripción base de Persona (polimorfismo). */
    @Override
    public String describir() {
        return super.describir() + " <" + email + ">";
    }

    // ---------- Regla 1 ----------

    public Optional<Membresia> getMembresiaVigente(LocalDate fecha) {
        for (Membresia m : membresias) {
            if (m.estaVigente(fecha)) return Optional.of(m);
        }
        return Optional.empty();
    }

    public boolean tieneMembresiaVigente(LocalDate fecha) {
        return getMembresiaVigente(fecha).isPresent();
    }

    /** Punto único donde se aplica la Regla 1; lo reutilizan la inscripción y la asistencia. */
    public void verificarMembresiaVigente(LocalDate fecha) throws MembresiaVencidaException {
        if (!tieneMembresiaVigente(fecha)) {
            throw new MembresiaVencidaException("Regla 1: " + getNombreCompleto()
                    + " no tiene membresía vigente al " + fecha + ".");
        }
    }

    /** Registra el ingreso del socio. Si la membresía está vencida, lanza MembresiaVencidaException. */
    public Asistencia registrarAsistencia(LocalDateTime momento, ClaseGrupal clase) throws MembresiaVencidaException {
        if (momento == null) throw new IllegalArgumentException("El momento de la asistencia no puede ser nulo.");
        verificarMembresiaVigente(momento.toLocalDate());
        return new Asistencia(0, this, clase, momento);
    }

    public void agregarMembresia(Membresia membresia) {
        if (membresia == null) throw new IllegalArgumentException("La membresía no puede ser nula.");
        membresias.add(membresia);
    }

    // ---------- Getters / setters con validación ----------

    public String getEmail() { return email; }

    public final void setEmail(String email) {
        String limpio = exigirTexto(email, "email");
        if (!limpio.contains("@") || limpio.startsWith("@") || limpio.endsWith("@")) {
            throw new IllegalArgumentException("El email no tiene un formato válido.");
        }
        this.email = limpio;
    }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }

    public final void setFechaNacimiento(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) throw new IllegalArgumentException("La fecha de nacimiento no puede ser nula.");
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        this.fechaNacimiento = fechaNacimiento;
    }

    public LocalDate getFechaAlta() { return fechaAlta; }

    /** Vista de solo lectura del historial de membresías. */
    public List<Membresia> getMembresias() { return Collections.unmodifiableList(membresias); }
}
