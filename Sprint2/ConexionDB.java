import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto ÚNICO de conexión a MySQL (conexión centralizada).
 * Si cambian host, base, usuario o contraseña, se modifica solo esta clase.
 *
 * Los valores pueden sobrescribirse sin tocar el código con variables de entorno:
 *   PPF_DB_URL, PPF_DB_USER, PPF_DB_PASS
 *
 * Requiere el driver MySQL Connector/J (archivo .jar) en el classpath.
 * En BlueJ: Herramientas → Preferencias → Bibliotecas → Agregar el .jar, y reiniciar BlueJ.
 */
public final class ConexionDB {

    private static final String URL = valor("PPF_DB_URL",
            "jdbc:mysql://localhost:3306/powerpitefit_db?sslMode=DISABLED&allowPublicKeyRetrieval=true");
    private static final String USUARIO = valor("PPF_DB_USER", "root");
    private static final String CLAVE = valor("PPF_DB_PASS", "");   // quitar contraseña antes de entregar

    private ConexionDB() { }   // clase utilitaria: no se instancia

    private static String valor(String variable, String porDefecto) {
        String v = System.getenv(variable);
        return (v == null || v.isEmpty()) ? porDefecto : v;
    }

    /** Abre una conexión nueva. Quien la pide debe cerrarla (try-with-resources). */
    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    /** Prueba rápida de conectividad: devuelve null si todo está bien o el motivo del error. */
    public static String probar() {
        try (Connection con = getConexion()) {
            return con.isValid(3) ? null : "La conexión se abrió pero no responde.";
        } catch (SQLException e) {
            return diagnosticar(e);
        }
    }

    /** Traduce los errores más comunes a un mensaje entendible. */
    public static String diagnosticar(SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (e.getErrorCode() == 1045) {
            return "Usuario o contraseña incorrectos (revisá PPF_DB_USER / PPF_DB_PASS o la constante CLAVE).";
        }
        if (e.getErrorCode() == 1049) {
            return "La base 'powerpitefit_db' no existe: ejecutá antes el script 01_esquema.sql.";
        }
        if (msg.contains("No suitable driver")) {
            return "Falta el driver JDBC: agregá el .jar de MySQL Connector/J al classpath.";
        }
        if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            return "No se pudo conectar: verificá que el servicio MySQL esté iniciado en localhost:3306.";
        }
        return "Error de base de datos (código " + e.getErrorCode() + "): " + msg;
    }
}
