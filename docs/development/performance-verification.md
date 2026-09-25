# Verificación del tiempo de respuesta

La prueba PNF01 verifica el requisito RNF04 mediante solicitudes HTTP reales a
una instancia del backend iniciada en un puerto aleatorio. Incluye las 29 rutas
de la API expuestas por los controladores de autenticación, administración,
equipos, tareas y recordatorios, además de `GET /actuator/health`. El criterio
de conformidad es que cada respuesta se complete en menos de dos segundos y
devuelva un código HTTP 2xx.

## Metodología automatizada

La prueba `AllEndpointsResponseTimeIntegrationTests` prepara administradores,
usuarios autenticados, dispositivos, equipos y un conjunto controlado de 100
tareas. Para cada endpoint ejecuta tres solicitudes de calentamiento y diez
mediciones. Las operaciones que consumen o modifican recursos reciben datos
nuevos antes de cada medición. Para cada ruta se registra:

- método HTTP y patrón de la ruta;
- cantidad de registros utilizada;
- número de calentamientos y diez repeticiones por endpoint;
- promedio;
- percentil 95;
- valor máximo;
- versión de Java;
- sistema operativo de ejecución.

La prueba consulta también el registro de rutas de Spring y compara ese
inventario con las rutas medidas. Si se agrega un endpoint nuevo sin añadir su
escenario de rendimiento, la prueba falla. También falla si una respuesta no
es HTTP 2xx o si el valor máximo es igual o superior a dos segundos. El
resultado aparece en el registro de Maven y en los reportes Surefire
conservados por el flujo de integración continua.

## Ejecución

Desde el directorio `backend`:

```bash
./mvnw --batch-mode -Dtest=AllEndpointsResponseTimeIntegrationTests test
```

Para una evidencia reproducible se debe registrar el commit evaluado y
conservar el reporte generado en `target/surefire-reports`. El pipeline publica
estos archivos en el artefacto `backend-test-evidence`.
