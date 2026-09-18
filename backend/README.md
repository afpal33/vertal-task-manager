# Vertal Backend

Backend self-hosted para gestión colaborativa de tareas. Usa Spring Boot, PostgreSQL, Spring Data JPA, Spring Security, JWT y Flyway. No usa contraseñas, correo, identidad externa, analítica ni telemetría.

## Ejecución con Docker

La instalación normal no requiere editar variables a mano:

```bash
cd backend
./setup.sh
docker compose up --build
```

`setup.sh` genera `.env`, una contraseña PostgreSQL, un secreto JWT y un par de claves local en `backend/secrets/`. Esos archivos están excluidos de Git. La clave privada permanece en el host y nunca se persiste en PostgreSQL.

Para una instalación automatizada o administrada, también puedes crear `.env` a partir de `.env.example` y proporcionar tus propios secretos antes de ejecutar Compose.

El backend queda disponible en `http://localhost:8080`. PostgreSQL solo se expone dentro de la red de Compose. Flyway ejecuta las migraciones `V1__init_schema.sql` y `V2__seed_admin.sql` al iniciar.

Para ejecutar localmente: `./mvnw test` usa H2; para producción se debe configurar PostgreSQL mediante las variables de `.env`.

## Arquitectura

El código está organizado en `config`, `controller`, `dto`, `exception`, `model`, `repository`, `security` y `service`. Los controladores solo coordinan DTOs; las reglas de autorización viven en `AuthorizationService` y en los servicios de dominio.

## Vinculación y autenticación

La aplicación genera localmente su par de claves. Envía solo la clave pública a `POST /api/auth/linking/request`. Un administrador aprueba o rechaza la solicitud. Tras la aprobación, el cliente pide un desafío en `POST /api/auth/challenge`, firma el valor recibido con su clave privada y envía la firma Base64 a `POST /api/auth/login`. El desafío expira en dos minutos y se elimina al intentar usarlo, por lo que no puede reutilizarse. El login exitoso devuelve un JWT que se envía como `Authorization: Bearer <token>`.

El seed crea el usuario `admin` sin clave privada. Para el primer arranque, la organización debe registrar la clave pública del dispositivo administrativo y crear su dispositivo/vinculación usando un procedimiento SQL controlado, o ejecutar una migración de bootstrap interna antes de exponer el servidor. El backend nunca genera ni almacena la clave privada.

## Endpoints principales

| Módulo | Rutas |
|---|---|
| Auth | `POST /api/auth/linking/request`, `POST /api/auth/challenge`, `POST /api/auth/login`, `GET /api/auth/me` |
| Admin | `GET /api/admin/linking/requests`, `POST /api/admin/linking/requests/{id}/approve`, `POST /api/admin/linking/requests/{id}/reject`, `GET /api/admin/users`, `PUT /api/admin/users/{id}/role`, `DELETE /api/admin/users/{id}` |
| Equipos | `POST /api/teams`, `GET /api/teams`, `GET /api/teams/{id}`, `POST /api/teams/{id}/members`, `DELETE /api/teams/{id}/members/{userId}` |
| Tareas | `POST /api/tasks`, `GET /api/tasks`, `GET /api/tasks/{id}`, `PUT /api/tasks/{id}`, `DELETE /api/tasks/{id}`, `POST /api/tasks/{id}/assignments` |
| Seguimiento | `PUT /api/tasks/{id}/status`, `PUT /api/tasks/{id}/complete`, `POST /api/tasks/{id}/reminders`, `GET /api/tasks/{id}/reminders`, `DELETE /api/reminders/{id}` |

Ejemplo de solicitud de vinculación:

```json
{
  "nombreUsuarioSolicitado": "ana",
  "nombreDispositivo": "Pixel de Ana",
  "identificadorDispositivo": "device-123",
  "clavePublicaDispositivo": "-----BEGIN PUBLIC KEY-----..."
}
```

Ejemplo de respuesta pendiente:

```json
{
  "id": 1,
  "nombreUsuarioSolicitado": "ana",
  "estado": "PENDIENTE",
  "fechaSolicitud": "2026-09-18T12:00:00Z",
  "clavePublicaServidor": null
}
```

## Roles y permisos

`ADMINISTRADOR_SISTEMAS` gestiona vinculaciones, roles y bajas, pero no obtiene permisos operativos automáticamente. `MANAGER` puede administrar equipos y tareas. `USUARIO_NORMAL` puede crear equipos y tareas en equipos donde participa. Solo miembros y usuarios con permisos de gestión pueden consultar o modificar recursos; las tareas siempre pertenecen a un equipo.

Los recordatorios solo guardan configuración. La notificación local la programa la aplicación móvil.

## Seguridad y limitaciones del MVP

Usar HTTPS en el proxy de la organización, rotar `JWT_SECRET`, proteger PostgreSQL y mantener la clave privada del servidor fuera de la base de datos. El almacenamiento de desafíos es temporal en memoria, por lo que un despliegue con varias réplicas requiere un almacén temporal interno compartido. La notificación y la generación de pares criptográficos quedan fuera del backend.