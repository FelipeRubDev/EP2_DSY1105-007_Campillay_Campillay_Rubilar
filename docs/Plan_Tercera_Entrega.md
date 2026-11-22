# Plan de la tercera entrega

Proyecto: MPAZ Notifica (DSY1105-007)
Base: commit `ef5793a Requisito N°3 completado`

Este plan cubre exactamente los seis puntos del feedback del profesor. No agrega requerimientos, refactorizaciones ni mejoras que no estén pedidas.

---

## 1. Los seis puntos y su traducción técnica

| # | Punto del feedback | Cambio concreto | Archivos principales |
|---|---|---|---|
| 1 | Reglas por perfil y error en el formulario | Profesor/a Jefe: solo su curso y sin circulares institucionales. Validación visible bajo el campo | `MpazNotificaViewModel.kt`, `RedactarComunicadoScreen.kt` |
| 2 | Completar el envío | Resolver destinatarios, simular despacho por canal y guardar estado por destinatario | `MpazNotificaViewModel.kt`, Room, `DetalleComunicadoScreen.kt` |
| 3 | Login con error por campo e íconos, sin "Modo desarrollo" | Validación por campo, íconos y eliminación del acceso directo | `LoginScreen.kt`, `MpazNotificaViewModel.kt` |
| 4 | Segundo recurso nativo y URI del PDF | Notificación local con permiso Android 13+ y persistencia del URI | Manifiesto, notificaciones, selector de PDF, Room |
| 5 | Animaciones y README | Transiciones de navegación, animaciones de estado y README real | Navegación, pantallas, `README.md` |
| 6 | Git y Gradle wrapper | Ramas por funcionalidad, commits pequeños de los tres integrantes y wrapper original | Repositorio y `gradle/wrapper` |

---

## 2. Punto de partida

Lo que hoy impide cumplir esos seis puntos:

- `alcancesPermitidos()` y `tiposPermitidos()` devuelven todas las opciones para cualquier perfil.
- `destinosPara()` ofrece todos los cursos, incluso al Profesor/a Jefe.
- `enviarComunicado()` solo inserta el comunicado. `_envios` sigue leyendo `MockData.envios`.
- `login()` siempre devuelve `false` y la pantalla ofrece "Modo desarrollo".
- El PDF guarda solo el nombre, no el URI.
- Solo existe una animación, el aviso de firma.
- El README afirma restricciones por perfil que el código no aplica.
- Hay un commit y un solo autor.

---

## 3. Diseño mínimo de cada punto

### 3.1 Reglas por perfil

| Perfil | Tipos permitidos | Alcances permitidos | Destinos |
|---|---|---|---|
| Inspectoría General | Todos | Los cuatro | Todos |
| Profesor/a Jefe | Todos menos `INSTITUCIONAL` | Solo `CURSO` | Solo su `cursoAsignado` |
| Sin sesión | Ninguno | Ninguno | Ninguno |

Se implementa directamente en las tres funciones que ya existen en el ViewModel:

```kotlin
fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = when (usuario?.perfil) {
    null -> emptyList()
    Perfil.INSPECTORIA_GENERAL -> TipoComunicacion.entries.toList()
    Perfil.PROFESOR_JEFE -> TipoComunicacion.entries.filter { it != TipoComunicacion.INSTITUCIONAL }
}

fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = when (usuario?.perfil) {
    null -> emptyList()
    Perfil.INSPECTORIA_GENERAL -> AlcanceEnvio.entries.toList()
    Perfil.PROFESOR_JEFE -> listOf(AlcanceEnvio.CURSO)
}
```

`destinosPara()` devuelve solo `usuario.cursoAsignado` cuando el alcance es `CURSO` y el perfil es Profesor/a Jefe.

La única exclusión de tipo es `INSTITUCIONAL`, que es la circular institucional. Los demás tipos siguen disponibles porque el requerimiento no pide bloquearlos. Tampoco se permite `COLEGIO_COMPLETO`, `TALLER` ni `APODERADO_INDIVIDUAL`: el Profesor/a Jefe solo emite a su curso.

El error se muestra en el formulario porque `SelectorDesplegable` ya recibe el parámetro `error` y pinta el mensaje bajo el campo. `validarComunicado()` ya devuelve los mensajes de tipo y alcance no permitidos; al dejar de ser universales las listas, esos errores pasan a ser alcanzables. No se agrega ninguna pantalla ni aviso nuevo.

### 3.2 Envío completo

Resolución de destinatarios según el alcance:

| Alcance | Destinatarios |
|---|---|
| `COLEGIO_COMPLETO` | Todos los apoderados |
| `CURSO` | Apoderados cuyos pupilos están en ese curso |
| `TALLER` | Apoderados cuyos pupilos están en ese taller |
| `APODERADO_INDIVIDUAL` | El apoderado cuyo nombre coincide con el destino |

Despacho simulado: por cada destinatario se usa su `canalPreferente` y se obtiene `DESPACHADO` o `FALLIDO`. Para que la demostración sea repetible, falla con teléfonos terminados en `2` y acierta con el resto. Es una simulación local, sin internet.

Persistencia: una tabla nueva `envios` con `id`, `comunicacionId`, `apoderadoId`, `canal` y `estado`. El comunicado y sus envíos se guardan en una sola transacción. Si no hay destinatarios, no se guarda nada y se muestra el error.

El detalle deja de leer `MockData.envios` y muestra los envíos reales de ese comunicado.

### 3.3 Login

Validación por campo:

| Campo | Regla | Mensaje |
|---|---|---|
| Correo | Vacío | "Ingresa tu correo institucional" |
| Correo | Sin `@` | "El correo no es válido" |
| Clave | Vacía | "Ingresa tu clave" |
| Clave | Menos de 6 caracteres | "La clave debe tener al menos 6 caracteres" |
| Ambos | No coincide con `MockData.usuarios` | "Correo o clave incorrectos" (error general) |

`login()` deja de devolver siempre `false` y compara contra los usuarios de prueba con el correo en minúsculas. La pantalla muestra `isError` y `supportingText` por campo, un error general debajo, íconos de correo y candado, y elimina los botones "Modo desarrollo" junto con `entrarModoDesarrollo()`.

Credenciales de prueba:

- `inspectoria@mpaznotifica.cl` / `inspectoria123`
- `profesor.4basicoA@mpaznotifica.cl` / `profesor123`

### 3.4 Recursos nativos

Notificación local:

- Permiso `POST_NOTIFICATIONS` en el manifiesto.
- Solicitud en tiempo de ejecución solo en Android 13 o superior, cuando el usuario ya entró al panel.
- Canal de notificación creado una vez.
- Se dispara después de guardar el comunicado, con el resumen de despachados y fallidos.
- Si el permiso se niega, el comunicado se guarda igual y no se muestra la notificación.

URI del PDF:

- Al elegir el archivo se conserva el permiso con `takePersistableUriPermission`.
- Se guarda `adjuntoPdfUri` en la entidad, además del nombre.
- En el detalle, tocar el adjunto abre el PDF con `Intent.ACTION_VIEW`. Si no hay app o el permiso ya no sirve, se muestra un mensaje y no se cierra la aplicación.

### 3.5 Datos

La versión de la base sube a 3. Se mantiene el `fallbackToDestructiveMigration()` que ya existe, sin agregar migraciones que no fueron pedidas.

Cambios:

- `comunicados`: columna nueva `adjuntoPdfUri TEXT`.
- `envios`: tabla nueva con cinco columnas.

### 3.6 Animaciones

Solo dos tipos, los que pide el feedback:

- Transiciones de navegación en el `NavHost` (entrada y salida, incluido el botón atrás).
- Animaciones de estado: contador del panel con `AnimatedContent`, tarjetas del historial con `animateItem()` y aparición del adjunto con `AnimatedVisibility`.

### 3.7 README

Se corrige para que diga lo que la app hace después de estos cambios:

- Credenciales de prueba y ausencia de "Modo desarrollo".
- Regla real del Profesor/a Jefe.
- Destinatarios y estado por envío.
- Notificación local y URI del PDF como recursos nativos.
- Regla de la simulación (teléfono terminado en 2 falla).
- Requerimientos que siguen pendientes, sin afirmar que están hechos.

---

## 4. Fases

Cada fase termina con el proyecto compilando.

### Fase 0. Gradle wrapper (20 min)

1. Restaurar la versión del wrapper que corresponde a AGP 8.5.2. El proyecto tiene 8.14.5, posterior al resto del template.
2. Regenerar el wrapper con esa distribución.
3. Verificar `./gradlew --version` y `./gradlew assembleDebug`.

Criterio: build verde con la versión restaurada.

Rama: `chore/wrapper`.
Commit: `chore(build): restaurar Gradle wrapper original`.

### Fase 1. Datos para envíos y URI (45 min)

1. Crear `EnvioDestinatarioEntity` y `EnvioDestinatarioDao`.
2. Agregar `adjuntoPdfUri` a `ComunicacionEntity`.
3. Subir `AppDatabase` a versión 3 y registrar el DAO nuevo.
4. En el repositorio, agregar el flujo de envíos y un método que guarde comunicado y envíos en una transacción.

Criterio: la app compila y el historial actual sigue funcionando.

Rama: `feat/room-envios`.
Commit: `feat(data): tabla de envios y uri del adjunto`.

### Fase 2. Login (45 min)

1. Agregar `ErroresLogin` al modelo.
2. Implementar `login()` con las reglas de la sección 3.3.
3. Reemplazar `errorLogin` por `erroresLogin` en la pantalla.
4. Agregar íconos y errores por campo.
5. Eliminar `entrarModoDesarrollo()` y los botones de la pantalla.
6. Actualizar el comentario de cabecera del ViewModel para que no hable de esqueleto.

Criterio: se entra solo con credenciales, cada campo muestra su error y el modo desarrollo ya no existe.

Rama: `feat/login`.
Commits: `feat(auth): login con validacion por campo`, `refactor(auth): quitar modo desarrollo`.

### Fase 3. Reglas por perfil (40 min)

1. Implementar las tres funciones de la sección 3.1.
2. Ajustar `validarComunicado()` para que use el usuario activo, como ya lo hace.
3. Verificar que los mensajes de tipo y alcance se pintan bajo el selector.
4. Actualizar el comentario que dice que la validación no devuelve errores.

Criterio: Profesor/a Jefe ve solo Curso, solo su curso y ningún tipo Institucional. Inspectoría no cambia.

Rama: `feat/perfiles`.
Commit: `feat(perfiles): restringir tipos y alcances del profesor jefe`.

### Fase 4. Envío (1 h 30)

1. Implementar la resolución de destinatarios para los cuatro alcances.
2. Implementar el despachador simulado por canal.
3. En `enviarComunicado()`, resolver destinatarios, despachar y guardar el comunicado con sus envíos en una transacción.
4. Exponer el estado del envío para que el diálogo de éxito espere al guardado real.
5. Reemplazar `_envios` por el flujo de Room en `enviosDe()`.
6. Mostrar en el detalle el canal y el estado de cada destinatario.
7. Si no hay destinatarios, mostrar el error y no guardar.

Criterio: colegio completo genera 3 envíos, 4° A genera 2, Taller de Robótica genera 1, apoderado individual genera 1. AP2 queda fallido. El detalle muestra los destinatarios de ese comunicado y no los de otro.

Rama: `feat/envio`.
Commits: `feat(envio): resolver destinatarios`, `feat(envio): simular despacho por canal`, `feat(envio): persistir estado por destinatario`.

### Fase 5. Recursos nativos (1 h)

1. Agregar el permiso de notificaciones al manifiesto.
2. Crear el canal y la función que publica la notificación.
3. Pedir el permiso en Android 13+ al entrar al panel.
4. Disparar la notificación cuando el envío termina.
5. Guardar el URI del PDF con permiso persistible.
6. Persistir `adjuntoPdfUri` y mapearlo al modelo.
7. Abrir el PDF desde el detalle.

Criterio: la notificación aparece con permiso y no bloquea nada si se niega. El PDF se abre después de cerrar y reabrir la app.

Rama: `feat/nativo`.
Commits: `feat(nativo): notificacion local`, `feat(adjunto): persistir uri del pdf`.

### Fase 6. Animaciones (40 min)

1. Transiciones en el `NavHost`.
2. Contador animado en el panel.
3. Tarjetas animadas en el historial.
4. Aparición del adjunto en el formulario.

Criterio: la navegación y los cambios de estado tienen animación, sin retrasar acciones.

Rama: `feat/animaciones`.
Commit: `feat(ui): transiciones y animaciones de estado`.

### Fase 7. README y cierre (40 min)

1. Reescribir el README según la sección 3.7.
2. Recorrer la lista de verificación de la sección 5.
3. Revisar que cada integrante tenga sus commits.

Rama: `docs/readme`.
Commit: `docs(readme): alinear estado real`.

---

## 5. Verificación

Solo se comprueba lo que piden los seis puntos.

| # | Punto | Comprobación |
|---|---|---|
| 1 | Perfiles | Login como Profesor: sin Institucional, solo Curso y su curso. Login como Inspectoría: todo |
| 1 | Error en formulario | Forzar un tipo o alcance no permitido muestra el mensaje bajo el campo |
| 2 | Destinatarios | Los cuatro alcances generan la cantidad esperada de envíos |
| 2 | Estado | AP2 aparece fallido y el resto despachado |
| 2 | Detalle | Cada comunicado muestra sus propios destinatarios |
| 3 | Login | Campos vacíos, correo inválido, clave corta y credenciales malas muestran su error |
| 3 | Sin modo desarrollo | No queda ningún botón de acceso directo |
| 4 | Notificación | Aparece con permiso; se guarda igual si se niega |
| 4 | URI | El PDF se abre desde el detalle después de cerrar la app |
| 5 | Animaciones | Hay transición al navegar y al cambiar el contador |
| 5 | README | Ninguna afirmación se contradice con la app |
| 6 | Build | `./gradlew assembleDebug` pasa |
| 6 | Wrapper | `./gradlew --version` muestra la versión restaurada |

La prueba de instalación limpia se hace una vez al final: desinstalar, instalar, recorrer el flujo completo.

---

## 6. Git

Sin push. Los commits y las ramas se crean localmente y el push lo hace el usuario al terminar.

### Ramas

Una rama por funcionalidad, tal como pide el profesor:

| Rama | Contenido | Autor sugerido |
|---|---|---|
| `chore/wrapper` | Gradle wrapper | Felipe |
| `feat/room-envios` | Tabla de envíos y URI | Felipe |
| `feat/login` | Login y eliminación del modo desarrollo | Angelo |
| `feat/perfiles` | Reglas por perfil | Angelo |
| `feat/envio` | Destinatarios y despacho | Felipe |
| `feat/nativo` | Notificación y URI | Brayan |
| `feat/animaciones` | Transiciones y animaciones | Brayan |
| `docs/readme` | README | Brayan |

### Orden de merge

`chore/wrapper` → `feat/room-envios` → `feat/login` → `feat/perfiles` → `feat/envio` → `feat/nativo` → `feat/animaciones` → `docs/readme`.

Cada rama sale de `main` actualizado y se integra antes de abrir la siguiente.

### Commits

Un tema por commit, mensaje en formato `tipo(alcance): descripción`. No se commitea código que no compila.

Cada integrante configura su identidad antes de commitear:

```bash
git config user.name "Nombre Apellido"
git config user.email "correo-asociado-a-github@ejemplo.cl"
```

El correo debe ser el de su cuenta de GitHub para que el commit quede asociado. Brayan y Angelo deben hacer sus propios commits, no firmarlos desde la sesión de Felipe.

---

## 7. Fuera de alcance

No se toca nada de esto, aunque aparezca en el diagnóstico del profesor, porque no está en las seis acciones pedidas:

- Filtros del historial (`historialFiltrado`). Se quedan como están.
- Edición del canal preferente del apoderado. El despacho solo lee el canal que ya tiene cada apoderado.
- Migraciones de Room. Se mantiene el fallback destructivo actual.
- Persistencia de `requiereFirmaFisica`.
- Inyección del repositorio por constructor.
- `rememberSaveable` en el formulario.
- Pruebas automatizadas nuevas.
- Borrado de archivos guía del template.
- Backend real o llamadas a servicios de mensajería.

---

## 8. Terminado cuando

- [ ] El Profesor/a Jefe no puede emitir a colegio completo, a otro curso ni circulares institucionales, y el error se ve en el formulario.
- [ ] Cada envío resuelve sus destinatarios y guarda un estado por apoderado.
- [ ] El login valida por campo, muestra íconos y no tiene "Modo desarrollo".
- [ ] La notificación local funciona con permiso en Android 13+.
- [ ] El URI del PDF se guarda y el archivo se reabre después de cerrar la app.
- [ ] Hay animaciones de transición y de estado.
- [ ] El README coincide con la app.
- [ ] El build pasa con el wrapper restaurado.
- [ ] Hay commits pequeños de los tres integrantes en ramas por funcionalidad.
