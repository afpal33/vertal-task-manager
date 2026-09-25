# Verificación de privacidad

## Controles implementados

- El código Flutter centraliza las peticiones HTTP en `ApiClient`.
- `ApiClient` solo acepta rutas relativas al servidor configurado y no sigue redirecciones HTTP.
- Cada petición autenticada incluye el identificador del dispositivo.
- El JWT queda vinculado al dispositivo que inició sesión.
- Un administrador puede revocar un dispositivo perdido. La revocación desactiva el dispositivo y su vinculación, por lo que el filtro de autenticación rechaza sus peticiones posteriores.
- La clave privada se almacena con `flutter_secure_storage`, configurado con RSA-OAEP y AES-GCM. En Android, la clave de cifrado RSA queda respaldada por Android Keystore.
- Android Backup está deshabilitado para evitar que las credenciales cifradas sean copiadas o restauradas en otro dispositivo sin su clave de Keystore.

## Auditoría automatizada

Desde `frontend`:

```bash
flutter test test/privacy_controls_test.dart
flutter pub deps
```

La suite comprueba la configuración criptográfica, el bloqueo de destinos externos y redirecciones, la ausencia de SDK conocidos de telemetría en los manifiestos de dependencias, la centralización de conexiones y la desactivación de copias de respaldo.

Desde `backend`:

```bash
./mvnw test
./mvnw -Dtest=BackendPrivacyTests test
./mvnw dependency:tree
```

`BackendPrivacyTests` comprueba que el backend no incorpore proveedores de
telemetría, no construya clientes de red salientes, no defina ni acepte la
clave privada del dispositivo y utilice una conexión configurable hacia
PostgreSQL autogestionado. Antes de ejecutar la prueba, Maven genera en
`target/privacy/runtime-dependencies.txt` el inventario completo de
dependencias de ejecución, incluidas las transitivas. La prueba rechaza tanto
los SDK de telemetría conocidos como cualquier grupo proveedor que no forme
parte de la lista revisada. Por tanto, la verificación no se limita a leer las
dependencias directas declaradas en `pom.xml`.

La auditoría conjunta y repetible se ejecuta desde la raíz del repositorio:

```bash
bash scripts/privacy_dependency_audit.sh
```

Las pruebas verifican la vinculación del JWT con el dispositivo y la revocación de un dispositivo perdido.

## Captura de tráfico en Android

La captura y la prueba de almacenamiento seguro pueden automatizarse desde la
raíz del repositorio cuando exista un AVD instalado:

```bash
bash scripts/android_rnf02_capture.sh \
  NOMBRE_DEL_AVD \
  http://10.0.2.2:8080 \
  docs/evidence/rnf02
```

El comando inicia un emulador limpio, genera `vertal-rnf02.pcap`, ejecuta la
prueba de integración que escribe y recupera la clave privada mediante Android
Keystore, comprueba una conexión real al servidor configurado y genera un
listado de destinos junto con un informe de ejecución.

La captura debe hacerse sobre una compilación de la versión evaluada, en un emulador limpio y con el servidor autogestionado configurado. El emulador de Android permite guardar todo su tráfico con la opción `-tcpdump`:

```bash
emulator @NOMBRE_DEL_AVD -tcpdump vertal-rnf02.pcap
flutter run
```

Durante la captura se deben ejecutar, como mínimo, estos flujos:

1. Generar la credencial local.
2. Solicitar vinculación.
3. Iniciar sesión.
4. Consultar equipos y tareas.
5. Crear o modificar una tarea.
6. Configurar un recordatorio local.
7. Mantener la aplicación abierta y en segundo plano durante un periodo definido.

Al terminar, se detiene el emulador y se analiza `vertal-rnf02.pcap` con Wireshark o `tshark`. El informe debe registrar fecha, versión o commit, servidor configurado, duración, flujos ejecutados y todos los destinos observados. El criterio de conformidad es que el tráfico atribuible a Vertal se dirija únicamente al servidor configurado. El tráfico propio del sistema operativo del emulador debe identificarse por separado y no atribuirse a la aplicación.

