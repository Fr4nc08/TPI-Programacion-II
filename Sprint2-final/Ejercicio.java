/** Un ejercicio dentro de una rutina (por ejemplo: Sentadilla 4x10, descanso 90 s). */
public class Ejercicio {
    private int id;
    private String nombre;
    private String grupoMuscular;
    private int series;
    private int repeticiones;
    private int descansoSegundos;

    public Ejercicio(int id, String nombre, String grupoMuscular, int series, int repeticiones, int descansoSegundos) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El ejercicio necesita un nombre.");
        }
        if (grupoMuscular == null || grupoMuscular.trim().isEmpty()) {
            throw new IllegalArgumentException("El ejercicio necesita un grupo muscular.");
        }
        if (series <= 0 || repeticiones <= 0 || descansoSegundos < 0) {
            throw new IllegalArgumentException("Series y repeticiones deben ser positivas; el descanso, no negativo.");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.grupoMuscular = grupoMuscular.trim();
        this.series = series;
        this.repeticiones = repeticiones;
        this.descansoSegundos = descansoSegundos;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getGrupoMuscular() { return grupoMuscular; }
    public int getSeries() { return series; }
    public int getRepeticiones() { return repeticiones; }
    public int getDescansoSegundos() { return descansoSegundos; }

    @Override
    public String toString() {
        return nombre + " (" + grupoMuscular + ") " + series + "x" + repeticiones
                + ", descanso " + descansoSegundos + " s";
    }
}
