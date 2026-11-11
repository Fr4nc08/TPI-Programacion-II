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
 * Demostración del dominio SIN base de datos. Sirve para probar en BlueJ / VS Code (o en la defensa)
 * que las reglas de negocio funcionan aunque MySQL no esté disponible.
 * Cada prueba imprime OK/FALLO y el programa termina con código 1 si alguna falla.
 *
 * Esquema de cada prueba (sin lambdas): se intenta la operación dentro de un try-catch,
 * se guarda el error en una variable (o null si salió bien) y se compara con lo esperado.
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
        for (Persona p : personas) {
            System.out.println("   " + p.describir());
        }

        // --- Regla 5: conflicto horario del entrenador ---
        System.out.println("\n-- Regla 5: un entrenador no puede dictar dos clases a la misma hora --");
        ClaseGrupal crossfit = new ClaseGrupal(1, "Crossfit", carlos, DayOfWeek.MONDAY, LocalTime.of(9, 0), 60, 2);
        ClaseGrupal funcional = new ClaseGrupal(2, "Funcional", carlos, DayOfWeek.MONDAY, LocalTime.of(9, 30), 45, 10);
        ClaseGrupal spinning = new ClaseGrupal(3, "Spinning", carlos, DayOfWeek.MONDAY, LocalTime.of(10, 0), 45, 10);
        ClaseGrupal otroDia = new ClaseGrupal(4, "Funcional", carlos, DayOfWeek.TUESDAY, LocalTime.of(9, 30), 45, 10);

        ReglaNegocioException error = null;

        try { carlos.asignarClase(crossfit); } catch (ReglaNegocioException e) { error = e; }
        esperarAceptacion("asignar Crossfit lunes 09:00-10:00 (primera clase)", error);

        error = null;
        try { carlos.asignarClase(funcional); } catch (ReglaNegocioException e) { error = e; }
        esperarRechazo("asignar Funcional lunes 09:30 (se superpone)", error);

        error = null;
        try { carlos.asignarClase(spinning); } catch (ReglaNegocioException e) { error = e; }
        esperarAceptacion("asignar Spinning lunes 10:00 (empieza justo cuando termina la otra)", error);

        error = null;
        try { carlos.asignarClase(otroDia); } catch (ReglaNegocioException e) { error = e; }
        esperarAceptacion("asignar Funcional martes 09:30 (otro día)", error);

        // --- Regla 3: cupo máximo (Crossfit tiene cupo 2) ---
        System.out.println("\n-- Regla 3: cupo máximo (Crossfit admite 2) --");

        error = null;
        try { crossfit.inscribir(ana, hoy); } catch (ReglaNegocioException e) { error = e; }
        esperarAceptacion("inscribir a Ana (1/2)", error);

        error = null;
        try { crossfit.inscribir(luis, hoy); } catch (ReglaNegocioException e) { error = e; }
        esperarAceptacion("inscribir a Luis (2/2)", error);

        error = null;
        try { crossfit.inscribir(marta, hoy); } catch (ReglaNegocioException e) { error = e; }
        esperarRechazo("inscribir a Marta (cupo lleno)", error);

        error = null;
        try { crossfit.inscribir(ana, hoy); } catch (ReglaNegocioException e) { error = e; }
        esperarRechazo("inscribir a Ana de nuevo (duplicado, el Set lo impide)", error);

        // --- Regla 1: membresía vencida ---
        System.out.println("\n-- Regla 1: sin membresía vigente no hay inscripción ni asistencia --");

        error = null;
        try { spinning.inscribir(vencido, hoy); } catch (ReglaNegocioException e) { error = e; }
        esperarRechazo("inscribir a Pedro (membresía vencida) en Spinning", error);

        error = null;
        try { vencido.registrarAsistencia(LocalDateTime.now(), null); } catch (ReglaNegocioException e) { error = e; }
        esperarRechazo("registrar asistencia de Pedro (membresía vencida)", error);

        error = null;
        try { marta.registrarAsistencia(LocalDateTime.now(), null); } catch (ReglaNegocioException e) { error = e; }
        esperarAceptacion("registrar asistencia de Marta (membresía vigente)", error);

        // --- Collections: Map para consultar socios por id ---
        System.out.println("\n-- Map<Integer, Socio>: búsqueda rápida por id --");
        Map<Integer, Socio> sociosPorId = new HashMap<>();
        sociosPorId.put(ana.getId(), ana);
        sociosPorId.put(luis.getId(), luis);
        sociosPorId.put(marta.getId(), marta);
        sociosPorId.put(vencido.getId(), vencido);
        System.out.println("   socio 12 → " + sociosPorId.get(12).getNombreCompleto());
        int activos = 0;
        for (Socio s : sociosPorId.values()) {
            if (s.tieneMembresiaVigente(hoy)) {
                activos++;
            }
        }
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
        if (fallos > 0) {
            System.exit(1);
        }
    }

    /** La operación debía ser rechazada: hay que haber recibido un error de regla de negocio. */
    private static void esperarRechazo(String descripcion, ReglaNegocioException error) {
        if (error != null) {
            verificar(descripcion + " → rechazado: " + error.getMessage(), true);
        } else {
            verificar(descripcion + " → debía rechazarse pero se aceptó", false);
        }
    }

    /** La operación debía aceptarse: no tiene que haber ningún error. */
    private static void esperarAceptacion(String descripcion, ReglaNegocioException error) {
        if (error == null) {
            verificar(descripcion + " → aceptado", true);
        } else {
            verificar(descripcion + " → debía aceptarse pero falló: " + error.getMessage(), false);
        }
    }

    private static void verificar(String descripcion, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println("   " + (ok ? "[OK]    " : "[FALLO] ") + descripcion);
    }
}
