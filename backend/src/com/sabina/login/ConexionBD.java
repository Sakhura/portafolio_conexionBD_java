package com.sabina.login;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones JDBC a MySQL leyendo credenciales desde config.properties
 * (archivo NO versionado; se crea a partir de config.properties.example).
 */
public class ConexionBD {

    private static final Properties config = cargarConfiguracion();

    private static Properties cargarConfiguracion() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(
                "No se pudo leer config.properties. Copia config.properties.example a config.properties "
                    + "y ajusta tus credenciales de MySQL.",
                e
            );
        }
        return props;
    }

    public static Connection obtenerConexion() throws SQLException {
        String url = config.getProperty("db.url");
        String usuario = config.getProperty("db.usuario");
        String password = config.getProperty("db.password");
        return DriverManager.getConnection(url, usuario, password);
    }
}
