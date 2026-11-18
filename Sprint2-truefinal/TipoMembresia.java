import java.math.BigDecimal;

public enum TipoMembresia {
    MENSUAL(1, 15000),
    TRIMESTRAL(3, 40000),
    ANUAL(12, 150000);

    private final int meses;
    private final BigDecimal precioBase;

    TipoMembresia(int meses, long precioBase) {
        this.meses = meses;
        this.precioBase = BigDecimal.valueOf(precioBase).setScale(2);
    }

    public int getMeses() { return meses; }
    public BigDecimal getPrecioBase() { return precioBase; }
}
