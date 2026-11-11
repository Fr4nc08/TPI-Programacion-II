import java.time.LocalDateTime;

/** Registro de que un socio entró al gimnasio, opcionalmente a una clase grupal. */
public class Asistencia {
    private int id;
    private final Socio socio;
    private final ClaseGrupal clase; // puede ser null: acceso libre a sala
    private final LocalDateTime fechaHora;

    public Asistencia(int id, Socio socio, ClaseGrupal clase, LocalDateTime fechaHora) {
        if (socio == null || fechaHora == null) {
            throw new IllegalArgumentException("La asistencia requiere socio y fecha/hora.");
        }
        this.id = id;
        this.socio = socio;
        this.clase = clase;
        this.fechaHora = fechaHora;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Socio getSocio() { return socio; }
    public ClaseGrupal getClase() { return clase; }
    public LocalDateTime getFechaHora() { return fechaHora; }

    @Override
    public String toString() {
        return "Asistencia de " + socio.getNombreCompleto() + " el " + fechaHora
                + (clase != null ? " a la clase " + clase.getNombre() : " (acceso libre)");
    }
}
