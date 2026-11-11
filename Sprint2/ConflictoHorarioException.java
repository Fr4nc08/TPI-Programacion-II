/** Regla 5: un entrenador no puede dictar dos clases en el mismo horario. */
public class ConflictoHorarioException extends ReglaNegocioException {
    public ConflictoHorarioException(String mensaje) {
        super(mensaje);
    }
}
