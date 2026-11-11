import java.util.Objects;

/**
 * Superclase abstracta del dominio. Concentra los datos comunes de Socio y Entrenador
 * (abstracción + herencia). Es abstracta porque en el gimnasio nunca existe una
 * "persona genérica": siempre es un socio o un entrenador.
 *
 * La identidad natural es el DNI (único), por eso equals/hashCode lo usan:
 * así un Set<Socio> evita duplicados aunque el objeto todavía no tenga id de base de datos.
 */
public abstract class Persona {
    private int id;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;

    protected Persona(int id, String nombre, String apellido, String dni, String telefono) {
        this.id = id;
        setNombre(nombre);
        setApellido(apellido);
        setDni(dni);
        setTelefono(telefono);
    }

    /** Cada subclase define su rol: es el método que se resuelve por polimorfismo. */
    public abstract String getRol();

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    /** Las subclases pueden sobrescribirlo (override) para agregar sus propios datos. */
    public String describir() {
        return getRol() + " #" + id + ": " + getNombreCompleto() + " (DNI " + dni + ")";
    }

    protected static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + campo + "' no puede ser nulo ni vacío.");
        }
        return valor.trim();
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getDni() { return dni; }
    public String getTelefono() { return telefono; }

    public final void setId(int id) {
        if (id < 0) throw new IllegalArgumentException("El id no puede ser negativo.");
        this.id = id;
    }

    public final void setNombre(String nombre) { this.nombre = exigirTexto(nombre, "nombre"); }
    public final void setApellido(String apellido) { this.apellido = exigirTexto(apellido, "apellido"); }

    public final void setDni(String dni) {
        String limpio = exigirTexto(dni, "dni");
        if (!limpio.matches("\\d{7,10}")) {
            throw new IllegalArgumentException("El DNI debe tener entre 7 y 10 dígitos numéricos.");
        }
        this.dni = limpio;
    }

    /** El teléfono es opcional: se guarda null si viene vacío. */
    public final void setTelefono(String telefono) {
        this.telefono = (telefono == null || telefono.trim().isEmpty()) ? null : telefono.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return dni.equals(((Persona) o).dni);
    }

    @Override
    public int hashCode() { return Objects.hash(dni); }

    @Override
    public String toString() { return describir(); }
}
