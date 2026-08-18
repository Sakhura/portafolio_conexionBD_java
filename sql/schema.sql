-- Esquema MySQL para el login de portafolio_conexionBD_java

CREATE DATABASE IF NOT EXISTS portafolio_login CHARACTER SET utf8mb4;
USE portafolio_login;

CREATE TABLE IF NOT EXISTS usuarios (
  id INT AUTO_INCREMENT PRIMARY KEY,
  correo VARCHAR(150) NOT NULL UNIQUE,
  password_hash CHAR(64) NOT NULL,
  salt VARCHAR(32) NOT NULL,
  nombre VARCHAR(100),
  creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Usuario de prueba (coincide con la pista del formulario): admin@demo.cl / admin123
-- password_hash = SHA-256(salt + password), generado con PasswordUtil.hashear()
INSERT INTO usuarios (correo, password_hash, salt, nombre) VALUES
('admin@demo.cl', '1fd8bee23fcd5f456795d000796b6dc0336f5cb9ed98887b6833406fb6f1027b', 'OrcYWWspn6j+fsZi', 'Administrador');
