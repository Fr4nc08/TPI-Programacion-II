import java.io.Console;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class MenuConsola {

    private final Scanner scanner = new Scanner(System.in, charsetEntrada());
    private final GimnasioService servicio = new GimnasioService();

    private static Charset charsetEntrada() {
        try {
            Console consola = System.console();
            if (consola != null) {
                return consola.charset();
            }
        } catch (RuntimeException e) {
            // se usa el juego de caracteres por defecto
        }
        return Charset.defaultCharset();
    }

    // ============================================================ menú principal

    public void iniciar() {
        try {
            int opcion;
            do {
                System.out.println("\n========== POWERPITEFIT - MENÚ PRINCIPAL ==========");
                System.out.println(" 1) Socio");
                System.out.println(" 2) Registrar entrenador");
                System.out.println(" 3) Crear clase grupal");
                System.out.println(" 4) Registrar asistencia");
                System.out.println(" 5) Listados");
                System.out.println(" 6) Precios de membresías");
                System.out.println(" 0) Salir");
                opcion = leerEntero("Elegí una opción: ");
                ejecutarPrincipal(opcion);
            } while (opcion != 0);
            System.out.println("¡Hasta luego!");
        } catch (NoSuchElementException e) {
            System.out.println("\nSe cerró la entrada de datos. Fin del programa.");
        }
    }

    private void ejecutarPrincipal(int opcion) {
        try {
            switch (opcion) {
                case 1:  menuSocio(); break;
                case 2:  registrarEntrenador(); break;
                case 3:  crearClase(); break;
                case 4:  registrarAsistencia(); break;
                case 5:  menuListados(); break;
                case 6:  menuPrecios(); break;
                case 0:  break;
                default: System.out.println("Opción inválida: elegí un número del menú.");
            }
        } catch (ReglaNegocioException | IllegalArgumentException | SQLException e) {
            informarError(e);
        }
    }

    // ================================================================ submenú Socio

    private void menuSocio() {
        int opcion;
        do {
            System.out.println("\n---------- SOCIO ----------");
            System.out.println(" 1) Registrar socio");
            System.out.println(" 2) Contratar / renovar membresía (con pago)");
            System.out.println(" 3) Inscribir socio a clase");
            System.out.println(" 4) Crear rutina para un socio");
            System.out.println(" 0) Volver al menú principal");
            opcion = leerEntero("Elegí una opción: ");
            try {
                switch (opcion) {
                    case 1:  registrarSocio(); break;
                    case 2:  contratarMembresia(); break;
                    case 3:  inscribirSocio(); break;
                    case 4:  crearRutina(); break;
                    case 0:  break;
                    default: System.out.println("Opción inválida: elegí un número del menú.");
                }
            } catch (ReglaNegocioException | IllegalArgumentException | SQLException e) {
                informarError(e);
            }
        } while (opcion != 0);
    }

    // ============================================================= submenú Listados

    private void menuListados() {
        int opcion;
        do {
            System.out.println("\n---------- LISTADOS ----------");
            System.out.println(" 1) Rutinas de un socio");
            System.out.println(" 2) Socios activos");
            System.out.println(" 3) Clases con cupos");
            System.out.println(" 4) Historial de pagos de un socio");
            System.out.println(" 0) Volver al menú principal");
            opcion = leerEntero("Elegí una opción: ");
            try {
                switch (opcion) {
                    case 1:  verRutinas(); break;
                    case 2:  servicio.listarSociosActivos(); break;
                    case 3:  servicio.listarClasesConCupos(); break;
                    case 4:  servicio.listarHistorialPagos(leerDni("DNI del socio: ")); break;
                    case 0:  break;
                    default: System.out.println("Opción inválida: elegí un número del menú.");
                }
            } catch (ReglaNegocioException | IllegalArgumentException | SQLException e) {
                informarError(e);
            }
        } while (opcion != 0);
    }

    // ======================================================= submenú Precios de membresías

    private void menuPrecios() {
        int opcion;
        do {
            System.out.println("\n---------- PRECIOS DE MEMBRESÍAS ----------");
            System.out.println(" 1) Ver precios actuales");
            System.out.println(" 2) Cambiar el precio de un plan");
            System.out.println(" 0) Volver al menú principal");
            opcion = leerEntero("Elegí una opción: ");
            try {
                switch (opcion) {
                    case 1:  mostrarPrecios(); break;
                    case 2:  cambiarPrecio(); break;
                    case 0:  break;
                    default: System.out.println("Opción inválida: elegí un número del menú.");
                }
            } catch (IllegalArgumentException | SQLException e) {
                informarError(e);
            }
        } while (opcion != 0);
    }

    // ================================================================ acciones

    private void registrarSocio() throws SQLException, ReglaNegocioException {
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String dni = leerDni("DNI (solo números): ");
        String telefono = leerTextoOpcional("Teléfono (opcional): ");
        String email = leerTexto("Email: ");
        LocalDate nacimiento = leerFecha("Fecha de nacimiento (AAAA-MM-DD): ");
        Socio s = servicio.registrarSocio(nombre, apellido, dni, telefono, email, nacimiento);
        System.out.println("[OK] Socio registrado: " + s);
    }

    private void registrarEntrenador() throws SQLException, ReglaNegocioException {
        String nombre = leerTexto("Nombre: ");
        String apellido = leerTexto("Apellido: ");
        String dni = leerDni("DNI (solo números): ");
        String telefono = leerTextoOpcional("Teléfono (opcional): ");
        String especialidad = leerTexto("Especialidad: ");
        Entrenador e = servicio.registrarEntrenador(nombre, apellido, dni, telefono, especialidad);
        System.out.println("[OK] Entrenador registrado: " + e);
    }

    private void contratarMembresia() throws SQLException, ReglaNegocioException {
        String dni = leerDni("DNI del socio: ");
        TipoMembresia tipo = leerEnum("Plan", TipoMembresia.values());
        MedioPago medio = leerEnum("Medio de pago", MedioPago.values());
        Pago pago = servicio.contratarMembresia(dni, tipo, medio);
        System.out.println("[OK] " + pago.getMembresia());
        System.out.println("[OK] " + pago);
    }

    private void crearClase() throws SQLException, ReglaNegocioException {
        String nombre = leerTexto("Nombre de la clase: ");
        String dniEntrenador = leerDni("DNI del entrenador: ");
        DayOfWeek dia = leerEnum("Día", DayOfWeek.values());
        LocalTime hora = leerHora("Hora de inicio (HH:MM): ");
        int duracion = leerEntero("Duración en minutos: ");
        int cupo = leerEntero("Cupo máximo: ");
        ClaseGrupal c = servicio.crearClase(nombre, dniEntrenador, dia, hora, duracion, cupo);
        System.out.println("[OK] Clase creada: " + c);
    }

    private void inscribirSocio() throws SQLException, ReglaNegocioException {
        String dni = leerDni("DNI del socio: ");
        int claseId = leerEntero("ID de la clase: ");
        ClaseGrupal c = servicio.inscribirSocio(dni, claseId);
        System.out.println("[OK] Inscripción realizada. " + c);
    }

    private void registrarAsistencia() throws SQLException, ReglaNegocioException {
        String dni = leerDni("DNI del socio: ");
        int claseId = leerEntero("ID de la clase (0 = acceso libre a sala): ");
        Asistencia a = servicio.registrarAsistencia(dni, claseId == 0 ? null : claseId);
        System.out.println("[OK] " + a);
    }

    private void crearRutina() throws SQLException, ReglaNegocioException {
        String dni = leerDni("DNI del socio: ");
        String dniEntrenador = leerDni("DNI del entrenador: ");
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
        Rutina r = servicio.crearRutina(dni, dniEntrenador, nombre, objetivo, ejercicios);
        System.out.println("[OK] Rutina guardada:\n" + r);
    }

    private void verRutinas() throws SQLException, ReglaNegocioException {
        List<Rutina> rutinas = servicio.rutinasDe(leerDni("DNI del socio: "));
        if (rutinas.isEmpty()) System.out.println("El socio no tiene rutinas.");
        for (Rutina r : rutinas) System.out.println(r);
    }

    private void mostrarPrecios() throws SQLException {
        Map<TipoMembresia, BigDecimal> precios = servicio.consultarPrecios();
        System.out.println("\nPrecios actuales de las membresías:");
        for (TipoMembresia tipo : TipoMembresia.values()) {
            BigDecimal precio = precios.get(tipo);
            String texto = (precio == null) ? "(sin precio cargado)" : "$" + precio.toPlainString();
            System.out.printf("  %-12s %s%n", tipo, texto);
        }
    }

    private void cambiarPrecio() throws SQLException {
        mostrarPrecios();
        System.out.println();
        TipoMembresia tipo = leerEnum("Plan a modificar", TipoMembresia.values());
        BigDecimal nuevo = leerPrecio("Nuevo precio (ej: 16500 o 16500,50): $");
        if (!leerSiNo("¿Confirmás cambiar el precio de " + tipo + " a $" + nuevo.toPlainString() + "?")) {
            System.out.println("Cambio cancelado. No se modificó ningún precio.");
            return;
        }
        servicio.cambiarPrecio(tipo, nuevo);
        System.out.println("[OK] Precio actualizado. Las membresías ya contratadas conservan el precio que pagaron.");
        mostrarPrecios();
    }

    // ================================================================ errores

    /** Muestra el error según su tipo y deja que el menú siga funcionando. */
    private void informarError(Exception e) {
        if (e instanceof ReglaNegocioException) {
            System.out.println("[NO PERMITIDO] " + e.getMessage());
        } else if (e instanceof SQLException) {
            System.out.println("[ERROR] " + ConexionDB.diagnosticar((SQLException) e));
        } else {
            System.out.println("[DATO INVÁLIDO] " + e.getMessage());
        }
    }


    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("  [ERROR] '" + linea + "' no es un número entero. Intentá de nuevo.");
            }
        }
    }

    private String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            if (!linea.isEmpty()) return linea;
            System.out.println("  [ERROR] Este dato es obligatorio.");
        }
    }

    private String leerTextoOpcional(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    private String leerDni(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine();
            try {
                return Persona.normalizarDni(linea);
            } catch (IllegalArgumentException e) {
                System.out.println("  [ERROR] " + e.getMessage());
            }
        }
    }

    private BigDecimal leerPrecio(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            if (!linea.matches("\\d{1,8}([.,]\\d{1,2})?")) {
                System.out.println("  [ERROR] Escribí solo números, sin separador de miles y con hasta 2 decimales "
                        + "(ej: 16500 o 16500,50).");
                continue;
            }
            try {
                return Membresia.validarPrecio(new BigDecimal(linea.replace(',', '.')));
            } catch (IllegalArgumentException e) {
                System.out.println("  [ERROR] " + e.getMessage());
            }
        }
    }

    private LocalDate leerFecha(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine().trim();
            try {
                return LocalDate.parse(linea);
            } catch (DateTimeParseException e) {
                System.out.println("  [ERROR] Fecha inválida. Usá el formato AAAA-MM-DD (ej: 1998-05-15).");
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
                System.out.println("  [ERROR] Hora inválida. Usá el formato HH:MM (ej: 09:30).");
            }
        }
    }

    private boolean leerSiNo(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (s/n): ");
            String linea = scanner.nextLine().trim().toLowerCase();
            if (linea.equals("s")) return true;
            if (linea.equals("n")) return false;
            System.out.println("  [ERROR] Respondé 's' o 'n'.");
        }
    }

    private <T extends Enum<T>> T leerEnum(String titulo, T[] valores) {
        System.out.println(titulo + ":");
        for (int i = 0; i < valores.length; i++) {
            System.out.println("  " + (i + 1) + ") " + valores[i]);
        }
        while (true) {
            int n = leerEntero("Elegí (1-" + valores.length + "): ");
            if (n >= 1 && n <= valores.length) return valores[n - 1];
            System.out.println("  [ERROR] Número fuera de rango.");
        }
    }
}
