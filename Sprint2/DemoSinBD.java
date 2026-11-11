import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demostración del dominio SIN base de datos. Sirve para probar en BlueJ (o en la defensa)
 * que las reglas de negocio funcionan aunque MySQL no esté disponible.
 * Cada prueba imprime OK/FALLO y el programa termina con código 1 si alguna falla.
 */
public class DemoSinBD {
    private static int fallos = 0;

    public static void main(String[] args) {
        System.out.println("=== POWERPITEFIT — DEMO DEL DOMINIO (sin base de datos) ===\n");
        LocalDate hoy = LocalDate.now();

        // --- Datos de prueba ---
        Entrenador carlos = new Entrenador(1, "Carlos", "Pérez", "30123456", "3534001122", "Crossfit");
        Socio ana = new Socio(10, "Ana", "Gómez", "40111222", "3534556677", "ana@mail.com",
                LocalDate.of(1998, 5, 15), hoy.minusMonths(6));
        Socio luis = new Socio(11, "Luis", "Díaz", "41222333", null, "luis@mail.com",
                LocalDate.of(1995, 8, 2), hoy.minusMonths(6));
        Socio marta = new Socio(12, "Marta", "Ruiz", "42333444", null, "marta@mail.com",
                LocalDate.of(2000, 1, 20), hoy.minusMonths(6));
        Socio vencido = new Socio(13, "Pedro", "Vega", "43444555", null, "pedro@mail.com",
                LocalDate.of(1990, 3, 3), hoy.minusMonths(6));

        ana.agregarMembresia(Membresia.nueva(TipoMembresia.MENSUAL, hoy.minusDays(5)));
        luis.agregarMembresia(Membresia.nueva(TipoMembresia.TRIMESTRAL, hoy.minusDays(10)));
        marta.agregarMembresia(Membresia.nueva(TipoMembresia.ANUAL, hoy.minusDays(30)));
        vencido.agregarMembresia(Membresia.nueva(TipoMembresia.MENSUAL, hoy.minusDays(60))); // venció hace ~30 días

        // --- Polimorfismo: una lista de Persona, cada una responde a su manera ---
        System.out.println("-- Polimorfismo (describir() se resuelve en tiempo de ejecución) --");
        List<Persona> personas = new ArrayList<>();
        personas.add(carlos);
        personas.add(ana);
        for (Persona p : personas) System.out.println("   " + p.describir());

        // --- Regla 5: conflicto horario del entrenador ---
        System.out.println("\n-- Regla 5: un entrenador no puede dictar dos clases a la misma hora --");
        ClaseGrupal crossfit = new ClaseGrupal(1, "Crossfit", carlos, DayOfWeek.MONDAY, LocalTime.of(9, 0), 60, 2);
        ClaseGrupal funcional = new ClaseGrupal(2, "Funcional", carlos, DayOfWeek.MONDAY, LocalTime.of(9, 30), 45, 10);
        ClaseGrupal spinning = new ClaseGrupal(3, "Spinning", carlos, DayOfWeek.MONDAY, LocalTime.of(10, 0), 45, 10);
        ClaseGrupal otroDia = new ClaseGrupal(4, "Funcional", carlos, DayOfWeek.TUESDAY, LocalTime.of(9, 30), 45, 10);
        try {
            carlos.asignarClase(crossfit);
            verificar("asignar Crossfit lunes 09:00-10:00 (primera clase)", true);
        } catch (ConflictoHorarioException e) {
            verificar("asignar Crossfit", false);
        }
        probarRechazo("asignar Funcional lunes 09:30 (se superpone)", () -> carlos.asignarClase(funcional));
        probarAceptacion("asignar Spinning lunes 10:00 (empieza justo cuando termina la otra)", () -> carlos.asignarClase(spinning));
        probarAceptacion("asignar Funcional martes 09:30 (otro día)", () -> carlos.asignarClase(otroDia));

        // --- Regla 3: cupo máximo (Crossfit tiene cupo 2) ---
        System.out.println("\n-- Regla 3: cupo máximo (Crossfit admite 2) --");
        probarAceptacion("inscribir a Ana (1/2)", () -> crossfit.inscribir(ana, hoy));
        probarAceptacion("inscribir a Luis (2/2)", () -> crossfit.inscribir(luis, hoy));
        probarRechazo("inscribir a Marta (cupo lleno)", () -> crossfit.inscribir(marta, hoy));
        probarRechazo("inscribir a Ana de nuevo (duplicado, el Set lo impide)", () -> crossfit.inscribir(ana, hoy));

        // --- Regla 1: membresía vencida ---
        System.out.println("\n-- Regla 1: sin membresía vigente no hay inscripción ni asistencia --");
        probarRechazo("inscribir a Pedro (membresía vencida) en Spinning", () -> spinning.inscribir(vencido, hoy));
        probarRechazo("registrar asistencia de Pedro (membresía vencida)",
                () -> vencido.registrarAsistencia(LocalDateTime.now(), null));
        probarAceptacion("registrar asistencia de Marta (membresía vigente)",
                () -> marta.registrarAsistencia(LocalDateTime.now(), null));

        // --- Collections: Map para consultar socios por id ---
        System.out.println("\n-- Map<Integer, Socio>: búsqueda rápida por id --");
        Map<Integer, Socio> sociosPorId = new HashMap<>();
        for (Socio s : List.of(ana, luis, marta, vencido)) sociosPorId.put(s.getId(), s);
        System.out.println("   socio 12 → " + sociosPorId.get(12).getNombreCompleto());
        long activos = sociosPorId.values().stream().filter(s -> s.tieneMembresiaVigente(hoy)).count();
        verificar("socios con membresía activa = 3 (obtenido: " + activos + ")", activos == 3);

        // --- Rutina y ejercicios ---
        System.out.println("\n-- Rutina con ejercicios --");
        Rutina rutina = new Rutina(0, "Fuerza base", "Ganar fuerza", ana, carlos, hoy);
        rutina.agregarEjercicio(new Ejercicio(0, "Sentadilla", "Piernas", 4, 10, 90));
        rutina.agregarEjercicio(new Ejercicio(0, "Press banca", "Pecho", 4, 8, 120));
        System.out.println("   " + rutina);
        verificar("la rutina tiene 2 ejercicios", rutina.getEjercicios().size() == 2);

        // --- Validaciones de datos (IllegalArgumentException) ---
        System.out.println("\n-- Validaciones de datos --");
        try {
            new Socio(99, " ", "X", "12345678", null, "x@x.com", LocalDate.of(2000, 1, 1), hoy);
            verificar("rechazar nombre vacío", false);
        } catch (IllegalArgumentException e) {
            verificar("rechazar nombre vacío → " + e.getMessage(), true);
        }
        try {
            new Pago(0, ana, ana.getMembresias().get(0), BigDecimal.ZERO, LocalDateTime.now(), MedioPago.EFECTIVO);
            verificar("rechazar pago de monto 0", false);
        } catch (IllegalArgumentException e) {
            verificar("rechazar pago de monto 0 → " + e.getMessage(), true);
        }

        System.out.println("\n" + (fallos == 0 ? "RESULTADO: todas las pruebas pasaron ✔" : "RESULTADO: " + fallos + " prueba(s) fallaron ✘"));
        if (fallos > 0) System.exit(1);
    }

    /** Operación que puede lanzar una regla de negocio. */
    private interface Operacion {
        void ejecutar() throws ReglaNegocioException;
    }

    private static void probarRechazo(String descripcion, Operacion op) {
        try {
            op.ejecutar();
            verificar(descripcion + " → debía rechazarse pero se aceptó", false);
        } catch (ReglaNegocioException e) {
            verificar(descripcion + " → rechazado: " + e.getMessage(), true);
        }
    }

    private static void probarAceptacion(String descripcion, Operacion op) {
        try {
            op.ejecutar();
            verificar(descripcion + " → aceptado", true);
        } catch (ReglaNegocioException e) {
            verificar(descripcion + " → debía aceptarse pero falló: " + e.getMessage(), false);
        }
    }

    private static void verificar(String descripcion, boolean ok) {
        if (!ok) fallos++;
        System.out.println("   " + (ok ? "[OK]    " : "[FALLO] ") + descripcion);
    }
}
