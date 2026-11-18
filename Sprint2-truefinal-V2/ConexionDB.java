import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionDB {

    private static final String URL = valor("PPF_DB_URL",
            "jdbc:mysql://localhost:3306/powerpitefit_db?sslMode=DISABLED&allowPublicKeyRetrieval=true"
            + "&characterEncoding=UTF-8");
    private static final String USUARIO = valor("PPF_DB_USER", "root");
    private static final String CLAVE = valor("PPF_DB_PASS", "");   // quitar contraseña asntes de entregar

    private ConexionDB() { }

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
        if (e.getErrorCode() == 1146) {
            return "Falta una tabla en la base de datos (" + msg + "). Si es 'precio_membresia', ejecutá "
                    + "04_migracion_precios.sql; si no, repetí 01_esquema.sql.";
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
