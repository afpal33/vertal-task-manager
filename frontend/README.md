# Vertal Mobile

Aplicación Flutter orientada a Android para el backend self-hosted de Vertal. No usa contraseñas, login social, correo, Firebase, analítica ni telemetría.

## Instalación y ejecución

Requisitos: Flutter estable, Dart incluido en Flutter y un dispositivo Android o emulador. Desde `frontend/`:

```bash
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=VERTAL_API_URL=http://10.0.2.2:8080
```

`VERTAL_API_URL` apunta al backend. En el emulador Android `10.0.2.2` representa el host; en un dispositivo físico se debe usar la dirección de red del servidor self-hosted.

### Linux desktop en Fedora

Linux desktop requiere dependencias nativas adicionales. En Fedora instala:

```bash
sudo dnf install clang cmake ninja-build pkgconf-pkg-config gtk3-devel libsecret-devel
```

Después reconstruye:

```bash
flutter clean
flutter pub get
flutter run -d linux
```

El proyecto también desactiva de forma localizada un warning antiguo de Clang emitido por `flutter_secure_storage_linux`; Android no se ve afectado.

### Entorno Android local

En este workspace el SDK está instalado en `frontend/.android-sdk` y Flutter usa Java 21 para Gradle. Si se recrea el entorno:

```bash
export ANDROID_HOME="$PWD/.android-sdk"
export ANDROID_SDK_ROOT="$PWD/.android-sdk"
export PATH="$PWD/.tools/flutter/bin:$PWD/.android-sdk/cmdline-tools/latest/bin:$PWD/.android-sdk/platform-tools:$PATH"
flutter config --android-sdk "$PWD/.android-sdk"
flutter config --jdk-dir=/usr/local/sdkman/candidates/java/21.0.12+1-ms
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-36" "platforms;android-35" "platforms;android-34" "build-tools;36.0.0" "build-tools;35.0.0"
```

El SDK y Flutter local están excluidos de Git. La compilación Android requiere además core library desugaring, configurado para `flutter_local_notifications`.

## Flujo de vinculación

1. La aplicación genera un par RSA local y guarda la clave privada con `flutter_secure_storage`.
2. Solo la clave pública se envía a `POST /api/auth/linking/request`.
3. El Administrador del Sistema aprueba la solicitud desde la sección Admin.
4. La aplicación solicita un desafío y lo firma localmente con la clave privada.
5. El JWT se guarda en almacenamiento seguro y se añade automáticamente a las llamadas REST.

La clave privada nunca se muestra ni se envía al backend. El backend sigue siendo la fuente final de permisos; la interfaz solo oculta acciones que no corresponden al rol conocido.

## Funcionalidades MVP

- Vinculación y login challenge-response sin contraseña.
- Listado y creación de equipos, con agregar o retirar miembros mediante su ID.
- Creación, edición, eliminación, asignación y seguimiento de tareas colaborativas.
- Estados pendiente, en progreso y completada.
- Recordatorios guardados en el backend y programados como notificaciones locales.
- Administración de solicitudes, roles y baja lógica de usuarios.
- Estados de carga, errores de red, sesión expirada y listas vacías.

## Estructura

```text
lib/
├── core/          # API, seguridad, almacenamiento, modelos y notificaciones
├── presentation/  # tema visual
├── pages/         # flujo de autenticación y workspace
├── widgets/       # componentes visuales compartidos
└── features/      # espacio para módulos de dominio
```

Los widgets no realizan llamadas HTTP. `AppController` coordina el estado de sesión y los servicios, mientras `ApiClient` centraliza el acceso REST.

## Pruebas y CI

Las pruebas se ejecutan con `flutter test` e incluyen generación de credenciales y compatibilidad con los contratos de roles y tareas. El workflow raíz ejecuta `flutter pub get`, `flutter analyze`, `flutter test` y la construcción del APK.

## Limitaciones del MVP

El backend actual no expone un endpoint de consulta detallada de miembros; por eso la pantalla usa IDs de usuario para agregar o retirar miembros. Los desafíos de autenticación se consumen una sola vez y los recordatorios dependen de permisos de notificación del sistema Android. No existe modo offline ni sincronización local-first.
