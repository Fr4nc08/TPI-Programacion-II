import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interfaz de usuario por consola. No contiene reglas de negocio ni SQL:
 * solo pide datos, los convierte con control de errores y delega en GimnasioService.
 *
 * Manejo de errores en tres niveles (de lo más específico a lo más general):
 *   1) Entrada del usuario  → NumberFormatException / DateTimeParseException / IllegalArgumentException
 *   2) Reglas de negocio    → ReglaNegocioException (y sus tres hijas)
 *   3) Base de datos        → SQLException
 */
public class MenuConsola {

    private final Scanner scanner = new Scanner(System.in);
    private final GimnasioService servicio = new GimnasioService();

    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEntero("Elegí una opción: ");
            ejecutar(opcion);
        } while (opcion != 0);
        System.out.println("¡Hasta luego!");
    }

    private void mostrarMenu() {
        System.out.println("\n========== POWERPITEFIT ==========");
        System.out.println(" 1) Registrar socio");
        System.out.println(" 2) Registrar entrenador");
        System.out.println(" 3) Contratar / renovar membresía (con pago)");
        System.out.println(" 4) Crear clase grupal");
        System.out.println(" 5) Inscribir socio a clase");
        System.out.println(" 6) Registrar asistencia");
        System.out.println(" 7) Crear rutina para un socio");
        System.out.println(" 8) Ver rutinas de un socio");
        System.out.println(" 9) Listar socios activos");
        System.out.println("10) Listar clases con cupos");
        System.out.println("11) Historial de pagos de un socio");
        System.out.println(" 0) Salir");
    }

    /** Un único try-catch por acción: cualquier error se informa y el menú sigue funcionando. */
    private void ejecutar(int opcion) {
        try {
            switch (opcion) {
                case 1:  registrarSocio(); break;
                case 2:  registrarEntrenador(); break;
                case 3:  contratarMembresia(); break;
                case 4:  crearClase(); break;
                case 5:  inscribirSocio(); break;
                case 6:  registrarAsistencia(); break;
                case 7:  crearRutina(); break;
                case 8:  verRutinas(); break;
                case 9:  servicio.listarSociosActivos(); break;
                case 10: servicio.listarClasesConCupos(); break;
                case 11: servicio.listarHistorialPagos(leerEntero("ID del socio: ")); break;
                case 0:  break;
                default: System.out.println("Opción inválida: elegí un número del menú.");
            }
        } catch (ReglaNegocioException e) {
            System.out.println("✘ No permitido: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("✘ Dato inválido: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("✘ " + ConexionDB.diagnosticar(e));
        }
    }

    // ------------------------------------------------------------- acciones

    private void registrarSocio() throws SQLException, ReglaNegocioException {
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String dni = leerTexto("DNI (solo números): ");
        String telefono = leerTextoOpcional("Teléfono (opcional): ");
        String email = leerTexto("Email: ");
        LocalDate nacimiento = leerFecha("Fecha de nacimiento (AAAA-MM-DD): ");
        Socio s = servicio.registrarSocio(nombre, apellido, dni, telefono, email, nacimiento);
        System.out.println("✔ Socio registrado: " + s);
    }

    private void registrarEntrenador() throws SQLException, ReglaNegocioException {
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String dni = leerTexto("DNI (solo números): ");
        String telefono = leerTextoOpcional("Teléfono (opcional): ");
        String especialidad = leerTexto("Especialidad: ");
        Entrenador e = servicio.registrarEntrenador(nombre, apellido, dni, telefono, especialidad);
        System.out.println("✔ Entrenador registrado: " + e);
    }

    private void contratarMembresia() throws SQLException, ReglaNegocioException {
        int socioId = leerEntero("ID del socio: ");
        TipoMembresia tipo = leerEnum("Plan", TipoMembresia.values());
        MedioPago medio = leerEnum("Medio de pago", MedioPago.values());
        Pago pago = servicio.contratarMembresia(socioId, tipo, medio);
        System.out.println("✔ " + pago.getMembresia());
        System.out.println("✔ " + pago);
    }

    private void crearClase() throws SQLException, ReglaNegocioException {
        String nombre = leerTexto("Nombre de la clase: ");
        int entrenadorId = leerEntero("ID del entrenador: ");
        DayOfWeek dia = leerEnum("Día", DayOfWeek.values());
        LocalTime hora = leerHora("Hora de inicio (HH:MM): ");
        int duracion = leerEntero("Duración en minutos: ");
        int cupo = leerEntero("Cupo máximo: ");
        ClaseGrupal c = servicio.crearClase(nombre, entrenadorId, dia, hora, duracion, cupo);
        System.out.println("✔ Clase creada: " + c);
    }

    private void inscribirSocio() throws SQLException, ReglaNegocioException {
        int socioId = leerEntero("ID del socio: ");
        int claseId = leerEntero("ID de la clase: ");
        ClaseGrupal c = servicio.inscribirSocio(socioId, claseId);
        System.out.println("✔ Inscripción realizada. " + c);
    }

    private void registrarAsistencia() throws SQLException, ReglaNegocioException {
        int socioId = leerEntero("ID del socio: ");
        int claseId = leerEntero("ID de la clase (0 = acceso libre a sala): ");
        Asistencia a = servicio.registrarAsistencia(socioId, claseId == 0 ? null : claseId);
        System.out.println("✔ " + a);
    }

    private void crearRutina() throws SQLException, ReglaNegocioException {
        int socioId = leerEntero("ID del socio: ");
        int entrenadorId = leerEntero("ID del entrenador: ");
        String nombre = leerTexto("Nombre de la rutina: ");
        String objetivo = leerTextoOpcional("Objetivo (opcional): ");
        List<Ejercicio> ejercicios = new ArrayList<>();
        do {
            System.out.println("--- Ejercicio #" + (ejercicios.size() + 1) + " ---");
            String ej = leerTexto("Nombre: ");
            String grupo = leerTexto("Grupo muscular: ");
            int series = leerEntero("Series: ");
            int reps = leerEntero("Repeticiones: ");
            int descanso = leerEntero("Descanso (segundos): ");
            ejercicios.add(new Ejercicio(0, ej, grupo, series, reps, descanso));
        } while (leerSiNo("¿Agregar otro ejercicio?"));
        Rutina r = servicio.crearRutina(socioId, entrenadorId, nombre, objetivo, ejercicios);
        System.out.println("✔ Rutina guardada:\n" + r);
    }

    private void verRutinas() throws SQLException, ReglaNegocioException {
        List<Rutina> rutinas = servicio.rutinasDe(leerEntero("ID del socio: "));
        if (rutinas.isEmpty()) System.out.println("El socio no tiene rutinas.");
        for (Rutina r : rutinas) System.out.println(r);
    }

    // ------------------------------------- lectura segura (repite hasta que sea válido)

    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("  ✘ '" + linea + "' no es un número entero. Intentá de nuevo.");
            }
        }
    }

    private String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            if (!linea.isEmpty()) return linea;
            System.out.println("  ✘ Este dato es obligatorio.");
        }
    }

    private String leerTextoOpcional(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    private LocalDate leerFecha(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return LocalDate.parse(linea);
            } catch (DateTimeParseException e) {
                System.out.println("  ✘ Fecha inválida. Usá el formato AAAA-MM-DD (ej: 1998-05-15).");
            }
        }
    }

    private LocalTime leerHora(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return LocalTime.parse(linea);
            } catch (DateTimeParseException e) {
                System.out.println("  ✘ Hora inválida. Usá el formato HH:MM (ej: 09:30).");
            }
        }
    }

    private boolean leerSiNo(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (s/n): ");
            String linea = scanner.nextLine().trim().toLowerCase();
            if (linea.equals("s")) return true;
            if (linea.equals("n")) return false;
            System.out.println("  ✘ Respondé 's' o 'n'.");
        }
    }

    /** Muestra las constantes de un enum numeradas y devuelve la elegida. */
    private <T extends Enum<T>> T leerEnum(String titulo, T[] valores) {
        System.out.println(titulo + ":");
        for (int i = 0; i < valores.length; i++) {
            System.out.println("  " + (i + 1) + ") " + valores[i]);
        }
        while (true) {
            int n = leerEntero("Elegí (1-" + valores.length + "): ");
            if (n >= 1 && n <= valores.length) return valores[n - 1];
            System.out.println("  ✘ Número fuera de rango.");
        }
    }
}
