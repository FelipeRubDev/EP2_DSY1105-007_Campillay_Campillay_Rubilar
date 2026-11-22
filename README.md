# MPAZ Notifica

Aplicación Android para reemplazar las circulares en papel de la Escuela Escritora Marcela Paz. El personal emisor redacta un comunicado, elige a quién se envía y queda una bitácora con autor, fecha, contenido, destinatarios y estado del despacho.

Proyecto de la asignatura DSY1105, sección 007.

## Integrantes

- Angelo Bastián Campillay Poblete
- Brayan Antonio Campillay Poblete
- Felipe Andrés Rubilar Olivares

Repositorio: https://github.com/FelipeRubDev/App_MpazNotifica

## Cómo ejecutar

1. Abrir el proyecto en Android Studio.
2. Usar JDK 17 para Gradle.
3. Sincronizar el proyecto.
4. Conectar un dispositivo o iniciar un emulador con API 24 o superior.
5. Ejecutar `Run 'app'`.

El wrapper usa Gradle 8.7, que corresponde a Android Gradle Plugin 8.5.2.

También se puede compilar por consola:

```bash
./gradlew assembleDebug
```

## Usuarios de prueba

| Perfil | Correo | Clave |
|---|---|---|
| Inspectoría General | `inspectoria@mpaznotifica.cl` | `inspectoria123` |
| Profesor/a Jefe | `profesor.4basicoA@mpaznotifica.cl` | `profesor123` |

No existe modo desarrollo ni acceso sin credenciales. Los datos son ficticios, según la Ley 19.628 y la Ley 21.719.

## Requerimientos implementados

| Requerimiento | Estado | Dónde |
|---|---|---|
| RF-01 Login diferenciado | Implementado con usuarios de prueba | `LoginScreen`, `MpazNotificaViewModel.login()` |
| RF-02 Restricción por perfil | Implementado | `tiposPermitidos()`, `alcancesPermitidos()`, `destinosPara()` |
| RF-03 Redacción con adjunto PDF | Implementado | `RedactarComunicadoScreen`, `ComunicacionEntity` |
| RF-04 Cuatro alcances | Implementado | Selector de alcance y resolución de destinatarios |
| RF-05 Inspectoría emite a todo | Implementado | Reglas por perfil |
| RF-06 Profesor solo a su curso | Implementado | Reglas por perfil |
| RF-07 Despacho simulado | Implementado | `DespachadorSimulado` |
| RF-08 Estado por destinatario | Implementado | Tabla `envios` |
| RF-09 Bitácora | Implementado | `HistorialScreen`, Room |
| RF-10 Filtros del historial | Pendiente | Los controles existen, la consulta no filtra |
| RF-11 Autor, fecha, destinatarios y canal | Implementado | `comunicados` y `envios` |
| RF-12 Firma física informativa | Parcial | Aviso en el formulario; el flag no se persiste |

## Reglas por perfil

| Perfil | Tipos | Alcances | Destinos |
|---|---|---|---|
| Inspectoría General | Todos | Los cuatro | Todos |
| Profesor/a Jefe | Todos menos Institucional | Solo Curso | Solo su curso asignado |

El formulario solo ofrece las opciones permitidas y la validación vuelve a comprobar el tipo y el alcance antes de enviar. Si el valor no está permitido, el mensaje aparece bajo el campo correspondiente.

## Cómo funciona el envío

1. Se validan asunto, cuerpo, tipo, alcance y destino.
2. Se resuelven los apoderados según el alcance: todos, los de un curso, los de un taller o uno individual.
3. Se simula el despacho por el canal preferente de cada apoderado.
4. El comunicado y un registro de envío por destinatario se guardan en una sola transacción.
5. El detalle muestra el canal y el estado de cada destinatario.

La simulación es local, no usa internet ni servicios reales. Para que la demostración sea repetible, los teléfonos terminados en `2` simulan un envío fallido y el resto se marca como despachado.

## Recursos nativos

- **Selector de archivos del sistema (Storage Access Framework).** Permite adjuntar un PDF sin pedir permisos de almacenamiento. El nombre real se obtiene con `OpenableColumns.DISPLAY_NAME`.
- **Notificación local.** Se publica al terminar un envío. En Android 13 o superior solicita el permiso `POST_NOTIFICATIONS`; si se niega, el comunicado se guarda igual.
- **Apertura de PDF con otra aplicación.** El URI del adjunto se conserva con permiso persistible y se abre desde el detalle con `Intent.ACTION_VIEW`.

## Arquitectura

```
UI Compose
   ↓
MpazNotificaViewModel
   ↓
ComunicacionRepository
   ↓
ComunicacionDao + EnvioDestinatarioDao
   ↓
Room / SQLite
```

- `MpazNotificaViewModel`: estado de sesión, validación, reglas por perfil, resolución de destinatarios y armado del comunicado.
- `ComunicacionRepository`: puerta única a los datos y transacción de comunicado más envíos.
- `ComunicacionEntity`: tabla `comunicados`, con asunto, cuerpo, tipo, alcance, destino, fecha, autor, nombre y URI del adjunto.
- `EnvioDestinatarioEntity`: tabla `envios`, con el canal y el estado por apoderado.
- `AppDatabase`: instancia única, versión 3.
