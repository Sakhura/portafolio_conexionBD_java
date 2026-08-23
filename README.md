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

## Paso a paso: cómo construir un login con backend real desde cero

Esta sección explica el **razonamiento** detrás de cada pieza — no solo cómo correr el código, sino por qué está armado así. Úsala como guía para replicar el patrón en tu propio proyecto.

### 1. Diseña el modelo de datos antes que el código

Antes de escribir una línea de Java, define qué necesitas guardar de un usuario. Acá basta con `correo` (único, para buscar al usuario), `password_hash` + `salt` (nunca la contraseña en texto plano) y `nombre` (ver [`sql/schema.sql`](sql/schema.sql)). Diseñar la tabla primero evita tener que rehacer el backend después de agregar una columna que se te olvidó.

### 2. Decide cómo vas a guardar contraseñas — y nunca en texto plano

[`PasswordUtil.java`](backend/src/com/sabina/login/PasswordUtil.java) genera un `salt` aleatorio por usuario y guarda `SHA-256(salt + password)`. El salt evita que dos usuarios con la misma contraseña tengan el mismo hash, y que un atacante use tablas precalculadas (*rainbow tables*) contra tu base. Esta es una versión simplificada con fines didácticos — en un proyecto real usa `BCrypt` o `Argon2`, que además son lentos a propósito (dificultan los ataques de fuerza bruta).

### 3. Arma el backend como una API, no como páginas

[`LoginServer.java`](backend/src/com/sabina/login/LoginServer.java) usa el servidor HTTP que trae el JDK (`com.sun.net.httpserver`) — sin Tomcat ni Spring Boot — para exponer un único endpoint: `POST /api/login`. La idea es que veas la mecánica cruda de una API antes de que un framework la esconda: leer el método HTTP, parsear el cuerpo de la petición, y responder JSON con un código de estado (`200`, `401`, `400`, `500`) según lo que pasó.

### 4. Usa siempre `PreparedStatement`, nunca concatenes SQL

En `LoginHandler.autenticar()`, la consulta usa `?` como parámetro y `ps.setString(1, correo)` en vez de armar el SQL con `+ correo +`. Esto evita inyección SQL: si concatenaras el correo directo en el string, alguien podría escribir `' OR '1'='1` en el campo de correo y saltarse el login completo.

### 5. Separa la configuración del código

[`ConexionBD.java`](backend/src/com/sabina/login/ConexionBD.java) lee la URL, usuario y password de MySQL desde `config.properties` — un archivo que **no se versiona** (está en `.gitignore`). Lo que sí se versiona es `config.properties.example`, la plantilla sin credenciales reales. Este patrón (`archivo.example` versionado, `archivo` real ignorado) es el que vas a usar en casi cualquier proyecto con secretos: nunca subas una contraseña real a GitHub, ni siquiera a un repo privado.

### 6. Define un contrato JSON consistente entre frontend y backend

Cada respuesta del backend tiene la misma forma: `{"ok": true/false, "mensaje": "..."}`. Así [`script.js`](login_bd_java/js/script.js) no necesita lógica distinta según qué salió mal — siempre lee `datos.ok` y `datos.mensaje`, y los muestra. Definir ese contrato antes de escribir el frontend evita ir cambiando ambos lados a cada rato.

### 7. Maneja el error de red aparte del error de credenciales

En `script.js`, el `catch` del `fetch()` es distinto de una respuesta `ok: false` — uno significa "el backend está apagado o no hay conexión", el otro "el backend respondió, pero el login era incorrecto". Mostrarlos igual confunde al usuario (y a ti, debuggeando).

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

## Checklist para tu propio backend con login

- [ ] Tabla de usuarios diseñada antes que el código (correo único, hash + salt)
- [ ] Contraseñas guardadas con hash + salt, nunca en texto plano
- [ ] Todas las consultas SQL usan `PreparedStatement` (nada de concatenar strings)
- [ ] Credenciales de BD en un archivo fuera de git (`.example` versionado, el real no)
- [ ] Contrato JSON consistente (`{ok, mensaje}`) en todas las respuestas
- [ ] Frontend distingue error de red vs. error de credenciales
- [ ] Probado con login correcto e incorrecto

## Stack usado

`Java (JDK, sin frameworks)` · `HttpServer` (JDK) · `JDBC` · `MySQL` · `HTML5` · `CSS3` · `JavaScript` (fetch API)
