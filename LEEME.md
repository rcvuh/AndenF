# Andén · T4.10 Probando Funcionalidad

## Novedades T4.10
1. **Activos de la red** (pestaña *Activos* en la barra inferior y botón en el Portal Administrador):
   catálogo con fotografía de unidades, andenes, tramos y estaciones del Tren El Insurgente,
   con filtro **Activos (en operación)** / **Inactivos (fuera de servicio)**. Incluye 5 activos y
   5 inactivos iniciales, cada uno con una foto distinta (`res/drawable-nodpi`).
2. **Administración (equipo desarrollador):** alta, edición, cambio de foto (galería), cambio de
   estado con un switch y eliminación. Todo se guarda primero en Room (tabla `network_assets`,
   BD versión 5) y después en Cloud Firestore (`activos/{assetId}`).
3. **Sincronización entre integrantes:** la app escucha `activos/` en tiempo real, así que lo que
   da de alta un integrante aparece en el teléfono de los demás. Si Cloud Storage no está
   disponible (plan Spark), la foto viaja como miniatura dentro del documento (`fotoMiniatura`).
4. **Equipo dado de alta como administrador:** `domain/TeamMembers.kt` (y la misma lista en
   `firebase/firestore.rules`). Al iniciar sesión cada integrante queda registrado en
   `usuarios/{uid}` con rol `ADMINISTRADOR`; cualquier otro correo entra como `PASAJERO`.
5. **Tarifas:** botón 💲 en *Activos de la red* con la tabla de tarifas por interestación.
6. **APK con el nombre de la app:** el workflow genera `Anden.apk` y lo publica en un Release con
   URL fija: `https://github.com/<usuario>/<repo>/releases/latest/download/Anden.apk`.
7. Paquete `anden.app.android` registrado en el proyecto Firebase `authapp-uaemex-7073`
   (`app/google-services.json`). Reglas combinadas en `firebase/`.
8. **29 pruebas unitarias** (se agregan 5 en `T410CatalogTest`).

---

# Andén · avance T4.8 (Desarrollo de Solución)

Proyecto Android Studio (Kotlin, Jetpack Compose, Material Design 3, MVVM, Room, Firebase, CameraX).

## Novedades en el Avance T4.8 (Desarrollo de Solución)
1. **Seguridad y Privacidad (`domain/SecurityPrivacyRules.kt`):**
   - Sanitización de entradas ciudadanas contra inyección XSS (`<script>`) y tokens SQL (`DROP TABLE`, `--`) con límite de 280 caracteres.
   - Seudonimización LFPDPPP de identificadores (`@h***z_anon`) y enmascaramiento de correo en la interfaz (`he***z@alumno.uaemex.mx`).
   - Ofuscación geoespacial de coordenadas GPS a 4 decimales (~11 m) en el feed público y validación RBAC de correos institucionales.
2. **Accesibilidad Universal WCAG 2.1 AA / TalkBack (`Theme.kt` y `SecurityPrivacyRules.kt`):**
   - Modo de Alto Contraste Accesible conmutable en tiempo real desde la interfaz.
   - Etiquetas semánticas `Modifier.semantics { contentDescription = ... }` para lectores de pantalla TalkBack en estaciones y geocerca.
3. **Centro Interactivo de Seguridad, Accesibilidad, Permisos Runtime y Material Design 3 (`SecurityAccessibilityFragmentScreen`):**
   - Nueva pantalla #9 en `ui/screens/AndenScreens.kt` con diagnóstico y solicitud en tiempo real de los 4 permisos (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA`, `RECORD_AUDIO`), probador en vivo de sanitización y conmutadores de privacidad y alto contraste.
4. **Suite ampliada a 24 Pruebas Unitarias JUnit 4:**
   - `AndenRulesTest.kt` (14 pruebas) + `SecurityPrivacyRulesTest.kt` (10 pruebas nuevas de Seguridad, Privacidad y Accesibilidad).
