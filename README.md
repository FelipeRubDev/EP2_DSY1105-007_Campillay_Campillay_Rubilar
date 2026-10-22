# EP2 - MPAZ Notifica (Sección DSY1105-007)

## Integrantes
- **Angelo Bastián Campillay Poblete**
- **Brayan Antonio Campillay Poblete**
- **Felipe Andres Rubilar Olivares**

## Repositorio GitHub
- **Enlace:https://github.com/FelipeRubDev/EP2_DSY1105-007_Campillay_Campillay_Rubilar.git** 

---

## Cómo ejecutar la aplicación
1. Abrir **Android Studio** (2024.1+ / Ladybug / Koala).
2. Abrir la carpeta raíz del proyecto `DSY1105-007_MpazNotifica`.
3. Asegurar que **Gradle JDK** esté configurado en **Java 17** (`Settings > Build, Execution, Deployment > Build Tools > Gradle`).
4. Sincronizar el proyecto (`Sync Project with Gradle Files`).
5. Iniciar un dispositivo virtual (Emulador Pixel 7 / API 35) o conectar un dispositivo físico Android.
6. Presionar **Run 'app'** (`Shift + F10`).

---

## Usuarios de prueba (Modo Desarrollo)
- **Inspectoría General:**
    - Acceso directo mediante el botón "Entrar como Inspectoría General".
    - Permite emitir comunicados a todos los alcances (Colegio completo, Cursos, Talleres, Apoderados).
- **Profesor/a Jefe:**
    - Acceso directo mediante el botón "Entrar como Profesor/a Jefe".
    - Alcance restringido únicamente a su curso de jefatura asignado (4° Básico A).

---

## Requerimientos Implementados
1. **Arquitectura MVVM + Repository:** Estructura en capas desacopladas (`UI` -> `ViewModel` -> `ComunicacionRepository` -> `ComunicacionDao` -> `AppDatabase`).
2. **Validaciones de formulario (RF-03):** Control de campos obligatorios, restricciones por perfil de usuario y límite de caracteres en el asunto.
3. **Persistencia Local con Room (SQLite):** Entidad `ComunicacionEntity`, DAO `ComunicacionDao`, base de datos `AppDatabase` y patrón `Repository` para almacenar comunicados de forma inmutable.
4. **Animaciones Jetpack Compose:** Implementación de `AnimatedVisibility` (`fadeIn` y `fadeOut`) para la retroalimentación dinámica de mensajes y avisos en el formulario de redacción.

---

## Recurso Nativo Utilizado
- **Recurso:** Storage Access Framework / Selector de archivos nativo del sistema Android (`ActivityResultContracts.OpenDocument`).
- **Justificación:** Permite al emisor adjuntar respaldos en formato PDF desde la memoria del dispositivo para acompañar citaciones o avisos oficiales. Cumple estrictamente con las restricciones del caso al no utilizar permisos invasivos de Cámara ni GPS.