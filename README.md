# CristalGiro

Launcher Android (Kotlin) con un cristal tipo Duo: **de frente el inicio se ve normal**; al girar en X el lado que se aleja se desenfoca un poco (los iconos se quedan, blandos).

Paquete: `com.soyache.blurgiro` · versión `0.2.0` (versionCode 6).

Sin anuncios, sin rastreo, **sin root**, **sin captura de pantalla** y **sin capa sobre otras apps**.

## Qué es (0.2.0)

CristalGiro **es la pantalla de inicio**. Pintamos wallpaper + iconos + dock y les aplicamos un StackBlur / degradado según el giroscopio, más un warp de perspectiva suave.

Por eso el efecto se parece a la foto de Héctor: somos dueños de esos píxeles. No leemos otras apps.

- De frente: identidad. Cero blur, cero warp.
- Giro en X hacia la derecha → la izquierda se pone un poco blanda; la derecha sigue más clara. Degradado a todo el ancho.
- Al abrir otra app el efecto se para. Es lo esperado: ya no somos un overlay.

## Por qué el overlay no podía funcionar

0.1.x intentó recubrir el teléfono con bandas `TYPE_APPLICATION_OVERLAY` y pedir `Window.setBackgroundBlurRadius` al compositor.

Eso **solo existe** si el fabricante compiló SurfaceFlinger con blur cruzado para terceros (`WindowManager.isCrossWindowBlurEnabled`). En Samsung, Xiaomi / HyperOS y la mayoría de OEM ese flag es falso. Su propia interfaz puede tener desenfoques; no se los prestan a overlays.

Otros caminos que Héctor rechazó, y que 0.2.0 **no** usa:

- `MediaProjection` (diálogo de grabar pantalla)
- `AccessibilityService` / `takeScreenshot`
- velo o niebla pintada
- mandarte a «activar desenfoque de ventana» en opciones de desarrollador (ese interruptor **no existe** si el OEM no compiló el flag)

La única forma honesta de hacer el cristal en esos teléfonos es **ser el launcher**.

## Compilar

Necesitas JDK 17+ y Android SDK (plataforma 35 y build-tools 35).

```bash
# Linux / macOS
export ANDROID_HOME=/ruta/al/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug test

# El APK queda en:
# app/build/outputs/apk/debug/app-debug.apk
```

En Windows (PowerShell), separa órdenes con `;`:

```powershell
$env:ANDROID_HOME = "C:\Users\TU_USUARIO\AppData\Local\Android\Sdk"
Set-Content -Path local.properties -Value "sdk.dir=$ANDROID_HOME"
.\gradlew.bat assembleDebug ; .\gradlew.bat test
```

Instalar en un teléfono con depuración USB:

```bash
./gradlew installDebug
```

También: `./gradlew assembleRelease`.

## Cómo usarlo en el teléfono

1. Instala el APK (depuración USB o sideload).
2. Android te pedirá una **app de inicio**, o ábrela desde el aviso en español de CristalGiro.
3. Elige **CristalGiro** → **Siempre** si te lo pide.
4. De frente no cambia nada. Gira el teléfono en X hacia la derecha: la **izquierda** se pone un poco blanda. Hacia la izquierda: al revés.
5. Toca un icono para abrir la app. El engranaje del dock abre intensidad y suavidad.

Volver al launcher anterior: Ajustes → apps predeterminadas → app de inicio.

## Permisos

| Permiso | Para qué |
| --- | --- |
| Visibilidad de paquetes (`QUERY_ALL_PACKAGES` + queries) | Listar apps instaladas con icono de launcher. |
| Almacenamiento (Android 12 e inferior) | Intentar leer el wallpaper del sistema. Si no se puede, usamos un fondo por defecto. |

No hay `SYSTEM_ALERT_WINDOW`. No hay servicio en primer plano. No hay `MediaProjection`. No hay accesibilidad.

## Límites (honesto)

- El cristal **solo** vive en el inicio de CristalGiro. Al salir a otra app no hay efecto. No es un fallo: ya no tapamos el sistema.
- Algunos OEM no dejan leer el wallpaper a un launcher de terceros. Entonces verás el fondo oscuro de la app; los iconos y el blur siguen.
- Android 11+ oculta paquetes si no declaramos queries. Traemos `QUERY_ALL_PACKAGES` y el intent `MAIN`/`LAUNCHER`.
- Gasta un poco de GPU/CPU al inclinar (StackBlur de la escena). En reposo no desenfoca.

## Privacidad

- Sin red, sin anuncios, sin analítica.
- Solo guarda en el teléfono intensidad, suavidad, modo y si ocultaste el aviso de «elegir inicio».
- No se incluyen secretos ni claves en el repositorio.

## Licencia de uso

Código para uso personal. Compílalo e instálalo tú; no se distribuye un binario firmado de producción en este repo.
