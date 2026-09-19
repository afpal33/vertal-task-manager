CREATE TABLE IF NOT EXISTS asignacion (
    id BIGSERIAL PRIMARY KEY,
    tarea_id BIGINT NOT NULL REFERENCES tarea(id),
    usuario_asignado_id BIGINT NOT NULL REFERENCES usuario(id),
    fecha_asignacion TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_asignacion_tarea_usuario UNIQUE (tarea_id, usuario_asignado_id)
);