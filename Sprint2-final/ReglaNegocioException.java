/**
 * Excepción base (checked) para cualquier violación de una regla de negocio.
 * Es checked a propósito: obliga a quien llama a decidir qué hacer (try-catch).
 */
public class ReglaNegocioException extends Exception {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
