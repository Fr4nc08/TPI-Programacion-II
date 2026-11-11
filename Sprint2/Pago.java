import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pago realizado por un socio para abonar una membresía. */
public class Pago {
    private int id;
    private final Socio socio;
    private final Membresia membresia;
    private final BigDecimal monto;
    private final LocalDateTime fechaHora;
    private final MedioPago medioPago;

    public Pago(int id, Socio socio, Membresia membresia, BigDecimal monto,
                LocalDateTime fechaHora, MedioPago medioPago) {
        if (socio == null || membresia == null || monto == null || fechaHora == null || medioPago == null) {
            throw new IllegalArgumentException("El pago requiere socio, membresía, monto, fecha y medio de pago.");
        }
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        this.id = id;
        this.socio = socio;
        this.membresia = membresia;
        this.monto = monto;
        this.fechaHora = fechaHora;
        this.medioPago = medioPago;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Socio getSocio() { return socio; }
    public Membresia getMembresia() { return membresia; }
    public BigDecimal getMonto() { return monto; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public MedioPago getMedioPago() { return medioPago; }

    @Override
    public String toString() {
        return "Pago #" + id + " de " + socio.getNombreCompleto() + ": $" + monto
                + " (" + medioPago + ") el " + fechaHora;
    }
}
