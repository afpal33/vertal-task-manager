INSERT INTO organizacion (nombre, fecha_creacion, activa) VALUES ('Organizacion inicial', CURRENT_TIMESTAMP, TRUE);
INSERT INTO servidor_autogestionado (nombre, direccion_red, clave_publica_servidor, activo, organizacion_id)
SELECT 'Servidor inicial', NULL, 'CONFIGURE_SERVER_PUBLIC_KEY', TRUE, id FROM organizacion WHERE nombre = 'Organizacion inicial';
INSERT INTO usuario (nombre_usuario, nombre_completo, rol, activo, fecha_registro, organizacion_id)
SELECT 'admin', 'Administrador inicial', 'ADMINISTRADOR_SISTEMAS', TRUE, CURRENT_TIMESTAMP, id FROM organizacion WHERE nombre = 'Organizacion inicial';