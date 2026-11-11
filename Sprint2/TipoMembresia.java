import java.math.BigDecimal;

/**
 * Planes de membresía. Cada constante conoce su duración en meses y su precio,
 * de modo que la regla "cuánto dura y cuánto cuesta" vive en un único lugar.
 */
public enum TipoMembresia {
    MENSUAL(1, 15000),
    TRIMESTRAL(3, 40000),
    ANUAL(12, 150000);

    private final int meses;
    private final BigDecimal precio;

    TipoMembresia(int meses, long precio) {
        this.meses = meses;
        this.precio = BigDecimal.valueOf(precio);
    }

    public int getMeses() { return meses; }
    public BigDecimal getPrecio() { return precio; }
}
