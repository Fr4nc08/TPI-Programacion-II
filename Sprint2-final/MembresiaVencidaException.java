/** Regla 1: no se permite asistencia ni inscripción con la membresía vencida. */
public class MembresiaVencidaException extends ReglaNegocioException {
    public MembresiaVencidaException(String mensaje) {
        super(mensaje);
    }
}
