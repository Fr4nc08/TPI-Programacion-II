/** Regla 3: no se permite inscribir a una clase grupal que superó su cupo máximo. */
public class CupoExcedidoException extends ReglaNegocioException {
    public CupoExcedidoException(String mensaje) {
        super(mensaje);
    }
}
