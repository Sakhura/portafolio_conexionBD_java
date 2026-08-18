package com.sabina.login;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Backend minimo con el servidor HTTP incluido en el JDK (sin Tomcat/Maven).
 * Expone POST /api/login y valida las credenciales contra MySQL.
 *
 * Compilar y ejecutar: ver README.md en la raiz del repositorio.
 */
public class LoginServer {

    public static void main(String[] args) throws IOException {
        int puerto = 8080;
        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/login", new LoginHandler());
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Backend escuchando en http://localhost:" + puerto);
    }

    static class LoginHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 405, false, "Metodo no permitido");
                return;
            }

            String cuerpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> datos = parsearFormulario(cuerpo);
            String correo = datos.getOrDefault("correo", "").trim();
            String password = datos.getOrDefault("password", "");

            if (correo.isEmpty() || password.isEmpty()) {
                responder(exchange, 400, false, "Correo y contrasena son obligatorios");
                return;
            }

            try {
                boolean valido = autenticar(correo, password);
                if (valido) {
                    responder(exchange, 200, true, "Bienvenido/a, inicio de sesion correcto");
                } else {
                    responder(exchange, 401, false, "Correo o contrasena incorrectos");
                }
            } catch (Exception e) {
                e.printStackTrace();
                responder(exchange, 500, false, "Error del servidor al validar el usuario");
            }
        }

        private boolean autenticar(String correo, String password) throws Exception {
            String sql = "SELECT password_hash, salt FROM usuarios WHERE correo = ?";
            try (Connection con = ConexionBD.obtenerConexion();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, correo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    String hashGuardado = rs.getString("password_hash");
                    String salt = rs.getString("salt");
                    String hashCalculado = PasswordUtil.hashear(password, salt);
                    return hashGuardado.equalsIgnoreCase(hashCalculado);
                }
            }
        }

        private Map<String, String> parsearFormulario(String cuerpo) {
            Map<String, String> datos = new HashMap<>();
            for (String par : cuerpo.split("&")) {
                if (par.isEmpty()) continue;
                String[] partes = par.split("=", 2);
                String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
                datos.put(clave, valor);
            }
            return datos;
        }

        private void responder(HttpExchange exchange, int codigo, boolean ok, String mensaje) throws IOException {
            String json = "{\"ok\":" + ok + ",\"mensaje\":\"" + escaparJson(mensaje) + "\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(codigo, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String escaparJson(String texto) {
            return texto.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
