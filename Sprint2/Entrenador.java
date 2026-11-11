import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Profesional que dicta clases y diseña rutinas. Aquí vive la Regla 5:
 * el entrenador conoce sus clases y rechaza cualquier superposición horaria.
 */
public class Entrenador extends Persona {
    private String especialidad;
    private final List<ClaseGrupal> clasesAsignadas = new ArrayList<>();

    public Entrenador(int id, String nombre, String apellido, String dni, String telefono, String especialidad) {
        super(id, nombre, apellido, dni, telefono);
        setEspecialidad(especialidad);
    }

    @Override
    public String getRol() { return "ENTRENADOR"; }

    /** Override: agrega la especialidad a la descripción base. */
    @Override
    public String describir() {
        return super.describir() + " — " + especialidad;
    }

    // ---------- Regla 5 ----------

    /** Devuelve la clase ya asignada que se superpone con la nueva, o null si no hay conflicto. */
    public ClaseGrupal buscarConflicto(ClaseGrupal nueva) {
        for (ClaseGrupal existente : clasesAsignadas) {
            if (existente != nueva && existente.seSolapaCon(nueva)) return existente;
        }
        return null;
    }

    /** Asigna una clase al entrenador validando la Regla 5. */
    public void asignarClase(ClaseGrupal nueva) throws ConflictoHorarioException {
        if (nueva == null) throw new IllegalArgumentException("La clase no puede ser nula.");
        ClaseGrupal conflicto = buscarConflicto(nueva);
        if (conflicto != null) {
            throw new ConflictoHorarioException("Regla 5: " + getNombreCompleto()
                    + " ya dicta '" + conflicto.getNombre() + "' (" + conflicto.getDiaSemana() + " "
                    + conflicto.getHoraInicio() + "-" + conflicto.getHoraFin()
                    + ") y se superpone con '" + nueva.getNombre() + "' (" + nueva.getDiaSemana() + " "
                    + nueva.getHoraInicio() + "-" + nueva.getHoraFin() + ").");
        }
        clasesAsignadas.add(nueva);
    }

    /**
     * Uso exclusivo de la capa de persistencia: reconstruye el estado leído de la base
     * sin volver a validar (esos datos ya fueron validados cuando se guardaron).
     */
    public void registrarClaseExistente(ClaseGrupal clase) {
        clasesAsignadas.add(clase);
    }

    public String getEspecialidad() { return especialidad; }

    public final void setEspecialidad(String especialidad) {
        this.especialidad = exigirTexto(especialidad, "especialidad");
    }

    public List<ClaseGrupal> getClasesAsignadas() { return Collections.unmodifiableList(clasesAsignadas); }
}
