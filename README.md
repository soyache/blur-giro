# CristalGiro

Efecto de cristal tipo Duo **en toda la interfaz del teléfono**, no solo en un launcher.

Paquete: `com.soyache.blurgiro` · versión **0.3.0** (versionCode 7).

Sin anuncios, sin rastreo, **sin root**. No pedimos el diálogo de «grabar pantalla» (`MediaProjection`).

La captura y la reproyección OpenGL siguen el enfoque que ya funciona en
[DuoFold-Android v0.6.0](https://github.com/jcx396905-gif/DuoFold-Android) (MIT, jcx / jcx396905-gif).
Ver [NOTICE.md](NOTICE.md) y `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.

## Qué es (0.3.0)

CristalGiro recubre **cualquier app** con una capa de accesibilidad. Un servicio
Shizuku (UserService con privilegio de shell) captura el fotograma con
`ScreenCapture` / `ScreenCaptureInternal` / `SurfaceControl` / captura antigua.
OpenGL ES lo vuelve a proyectar con el giroscopio / game-rotation-vector:
perspectiva + blur esmerilado direccional.

- De frente: identidad. Cero warp, la capa se vuelve transparente.
- Giro en X: el lado que se aleja se pone blando; el frontal sigue limpio.
- La capa del efecto se excluye de la captura (`setSkipScreenshot` / `setExcludeLayers`) para no recogerse a sí misma.
- Si la captura falla, **no** pintamos una niebla falsa: verás los pasos en español (Shizuku + accesibilidad).

El launcher-home de 0.2.0 ya no es el producto. Esta versión no se registra como app de inicio.

## Sabores

| APK | minSdk | Uso |
| --- | --- | --- |
| `standard` | 34 (Android 14+) | Captura en vivo + corrección táctil de accesibilidad |
| `compat` | 29 (Android 10–13) | Mismos backends con sondeo; en 10/11 la capa no se puede excluir por capa, así que se congela un fotograma limpio al arrancar |

Una sola base de código: el puente prueba los backends en tiempo de ejecución.

## Requisitos

- Android 10 o superior (14+ para el sabor estándar).
- [Shizuku](https://github.com/RikkaApps/Shizuku) instalado y **en marcha** (depuración inalámbrica o ADB). **No hace falta root.**
- Servicio de accesibilidad de CristalGiro activado.

## Cómo usarlo

1. Instala el APK que corresponda a tu Android.
2. Abre Shizuku e inicia el servicio.
3. En CristalGiro: **01 Instalar e iniciar Shizuku** (abre Shizuku) → **02 Autorizar Shizuku**.
4. **03 Activar accesibilidad**: busca CristalGiro y actívalo.
5. Sujeta el teléfono de frente y pulsa **04 Probar 10 segundos**. Inclina en X.
6. Si te convence, **05 Activar efecto global**.
7. Android 14+: tres dedos a la vez, o «Detener» en la notificación. Android 10–13: la notificación.

Si la accesibilidad aparece conectada pero el efecto no arranca, apágala y vuélvela a encender. En algunos Xiaomi la corrección táctil pide **Depuración USB (ajustes de seguridad)**.

## Compilar

JDK 17+ y Android SDK (plataforma 35, build-tools 35).

```bash
export ANDROID_HOME=/ruta/al/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug assembleRelease :motion:test :app:test
```

En Windows (PowerShell), separa órdenes con `;`:

```powershell
$env:ANDROID_HOME = "C:\Users\TU_USUARIO\AppData\Local\Android\Sdk"
Set-Content -Path local.properties -Value "sdk.dir=$ANDROID_HOME"
.\gradlew.bat assembleDebug ; .\gradlew.bat assembleRelease ; .\gradlew.bat :motion:test
```

APKs:

- `app/build/outputs/apk/standard/debug/app-standard-debug.apk`
- `app/build/outputs/apk/compat/debug/app-compat-debug.apk`
- equivalentes `release/`

## Permisos

| Permiso / servicio | Para qué |
| --- | --- |
| Shizuku (UserService) | Captura privilegiada e inyección táctil. Sin diálogo de MediaProjection. |
| Accesibilidad | Capa a pantalla completa y, en Android 14+, corrección de toques. |
| Notificaciones | Botón para detener el efecto. |

Sin `SYSTEM_ALERT_WINDOW`. Sin red. Sin root.

## Límites (honesto)

- Contenido con `FLAG_SECURE` o protegido puede salir negro. El efecto se corta y vuelve a la pantalla original.
- Android 10/11 no tiene exclusión por capa: el sabor compat anima **un** fotograma real capturado al arrancar (o al rotar).
- Android 10–13 no tienen la API de corrección táctil de Android 14. Si cuesta pulsar inclinado, vuelve a la pose calibrada.
- Gasta GPU y batería. Tope de captura 1080 px de ancho y 30 FPS, como DuoFold.
- OEM distintos pueden limitar captura o capas. Probado por DuoFold en Xiaomi 15 / Android 16; tu fabricante puede variar.
- No reordena iconos ni layouts de otras apps: solo cambia el fotograma final.

## Privacidad

- Sin permiso de Internet.
- La imagen de pantalla no se guarda ni se sube.
- Solo se guardan en el teléfono intensidad, ángulo, eje Z, estilo y el último estado.

## Atribución

Arquitectura de captura / overlay / GL adaptada de **DuoFold** (MIT) de
[jcx396905-gif](https://github.com/jcx396905-gif/DuoFold-Android). CristalGiro
cambia paquete, marca y textos; no quita sus avisos de copyright.
