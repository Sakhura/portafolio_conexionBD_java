# portafolio_conexionBD_java

Login sencillo (HTML + CSS + JS) conectado a un backend en Java puro que valida
las credenciales contra una base de datos MySQL vía JDBC.

## Estructura del repositorio

```
├── login_bd_java/        # Frontend estático
│   ├── index.html
│   ├── css/style.css
│   └── js/script.js      # Hace fetch() a POST /api/login
├── backend/               # Backend en Java (sin Maven ni Tomcat, solo JDK + driver JDBC)
│   ├── src/com/sabina/login/
│   │   ├── LoginServer.java   # Servidor HTTP embebido (com.sun.net.httpserver), expone /api/login
│   │   ├── ConexionBD.java    # Abre la conexión JDBC leyendo config.properties
│   │   └── PasswordUtil.java  # Hash SHA-256 + salt
│   ├── config.properties.example
│   └── lib/                   # Aquí va el driver mysql-connector-j (no se versiona)
└── sql/
    └── schema.sql          # Tabla `usuarios` + usuario de prueba
```

## Cómo funciona

1. El formulario de `login_bd_java/index.html` envía `correo` y `password` por
   `fetch()` (form-urlencoded) a `http://localhost:8080/api/login`.
2. `LoginServer` recibe la petición, busca el usuario en MySQL por correo,
   recalcula el hash de la contraseña con el `salt` guardado y lo compara.
3. Responde JSON `{"ok": true/false, "mensaje": "..."}`, que el frontend
   muestra en pantalla.

Las contraseñas **no se guardan en texto plano**: se almacena `SHA-256(salt + password)`
junto con el `salt` de cada usuario. Es una versión simplificada con fines
didácticos; en un proyecto real se recomienda BCrypt o Argon2.

## 1. Preparar la base de datos

Con MySQL corriendo localmente:

```bash
mysql -u root -p < sql/schema.sql
```

Esto crea la base `portafolio_login`, la tabla `usuarios` y un usuario de
prueba: **admin@demo.cl / admin123** (el mismo que sugiere el formulario).

## 2. Configurar credenciales del backend

```bash
cp backend/config.properties.example backend/config.properties
```

Y edita `backend/config.properties` con tu usuario/contraseña de MySQL:

```properties
db.url=jdbc:mysql://localhost:3306/portafolio_login?useSSL=false&serverTimezone=UTC
db.usuario=root
db.password=tu_password
```

## 3. Descargar el driver JDBC de MySQL

Descarga el jar de **MySQL Connector/J** (por ejemplo `mysql-connector-j-9.x.x.jar`)
desde [dev.mysql.com](https://dev.mysql.com/downloads/connector/j/) o
[Maven Central](https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/)
y colócalo en `backend/lib/`.

## 4. Compilar y ejecutar el backend

Desde la carpeta `backend/`:

```bash
# Windows
javac -d out src\com\sabina\login\*.java
java -cp "out;lib\mysql-connector-j-9.1.0.jar" com.sabina.login.LoginServer

# macOS / Linux
javac -d out src/com/sabina/login/*.java
java -cp "out:lib/mysql-connector-j-9.1.0.jar" com.sabina.login.LoginServer
```

Deberías ver: `Backend escuchando en http://localhost:8080`

`config.properties` se lee desde el directorio actual, así que ejecuta el
`java -cp ...` estando parado en `backend/`.

## 5. Abrir el frontend

Abre `login_bd_java/index.html` directamente en el navegador (o sírvelo con
cualquier servidor estático). Con el backend corriendo, prueba con
**admin@demo.cl / admin123**.

## Notas de seguridad (versión educativa)

- CORS abierto (`Access-Control-Allow-Origin: *`) para simplificar las
  pruebas locales; en producción se debería restringir el origen.
- El backend usa `PreparedStatement` en todas las consultas para evitar
  inyección SQL.
- `config.properties` está en `.gitignore`: nunca subas credenciales reales
  al repositorio.
