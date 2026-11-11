/**
 * Punto de entrada. Antes de mostrar el menú verifica la conexión con MySQL
 * y, si falla, explica el motivo en lugar de mostrar un error críptico.
 *
 * En BlueJ: clic derecho sobre la clase Main → void main(String[] args).
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== POWERPITEFIT — Sistema de gestión de gimnasio ===");
        String problema = ConexionDB.probar();
        if (problema != null) {
            System.out.println("✘ No se pudo conectar a la base de datos.");
            System.out.println("  Motivo: " + problema);
            System.out.println("  (Podés probar las reglas de negocio sin base de datos ejecutando DemoSinBD.)");
            return;
        }
        System.out.println("✔ Conexión con MySQL establecida.");
        new MenuConsola().iniciar();
    }
}
