import java.math.BigDecimal;
import java.time.LocalDate;

public class Membresia {
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

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

    /** Fábrica: calcula el vencimiento a partir del plan y usa el precio que se le indica (el vigente en la base). */
    public static Membresia nueva(TipoMembresia tipo, LocalDate inicio, BigDecimal precio) {
        return new Membresia(0, tipo, inicio, inicio.plusMonths(tipo.getMeses()), precio);
    }

    /** Variante con el precio base del plan; la usan las pruebas sin base de datos. */
    public static Membresia nueva(TipoMembresia tipo, LocalDate inicio) {
        return nueva(tipo, inicio, tipo.getPrecioBase());
    }

    /**
     * Regla del precio de un plan: mayor a cero, con a lo sumo 2 decimales y que entre en DECIMAL(10,2).
     * Devuelve el precio con exactamente 2 decimales.
     */
    public static BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio no puede ser nulo.");
        }
        if (precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El precio admite como máximo 2 decimales.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new IllegalArgumentException("El precio no puede superar " + PRECIO_MAXIMO.toPlainString() + ".");
        }
        return precio.setScale(2);
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
        return "Membresía " + tipo + " [" + fechaInicio + " a " + fechaVencimiento + "] $" + precio;
    }
}
