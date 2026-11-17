public class Main {
    public static void main(String[] args) {
        System.out.println("=== POWERPITEFIT - Sistema de gestión de gimnasio ===");
        String problema = ConexionDB.probar();
        if (problema != null) {
            System.out.println("[ERROR] No se pudo conectar a la base de datos.");
            System.out.println("  Motivo: " + problema);
            System.out.println("  (Podés probar las reglas de negocio sin base de datos ejecutando DemoSinBD.)");
            return;
        }
        System.out.println("[OK] Conexión con MySQL establecida.");
        new MenuConsola().iniciar();
    }
}
