import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Período de acceso al gimnasio. Se usa BigDecimal para el precio porque el dinero
 * no debe representarse con double (errores de redondeo binario).
 */
public class Membresia {
    private int id;
    private TipoMembresia tipo;
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private BigDecimal precio;

    public Membresia(int id, TipoMembresia tipo, LocalDate fechaInicio, LocalDate fechaVencimiento, BigDecimal precio) {
        if (tipo == null || fechaInicio == null || fechaVencimiento == null || precio == null) {
            throw new IllegalArgumentException("La membresía requiere tipo, fechas y precio.");
        }
        if (!fechaVencimiento.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("El vencimiento debe ser posterior al inicio.");
        }
        if (precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        this.id = id;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.precio = precio;
    }

    /** Fábrica: calcula vencimiento y precio a partir del plan elegido. */
    public static Membresia nueva(TipoMembresia tipo, LocalDate inicio) {
        return new Membresia(0, tipo, inicio, inicio.plusMonths(tipo.getMeses()), tipo.getPrecio());
    }

    /** Vigente si la fecha cae dentro del período, ambos extremos inclusive. */
    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaVencimiento);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public TipoMembresia getTipo() { return tipo; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public BigDecimal getPrecio() { return precio; }

    @Override
    public String toString() {
        return "Membresía " + tipo + " [" + fechaInicio + " → " + fechaVencimiento + "] $" + precio;
    }
}
