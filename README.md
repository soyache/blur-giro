# CristalGiro

App Android (Kotlin) que pone **todo el teléfono** bajo un cristal tipo Duo: de frente se ve normal; al girar en X el lado que se aleja se desenfoca un poco (los iconos se quedan, blandos).

Paquete: `com.soyache.blurgiro` · versión `0.1.3` (versionCode 4).

Sin anuncios, sin rastreo, **sin root** y **sin captura de pantalla**.

## Qué hace

- Varias bandas flotantes (`TYPE_APPLICATION_OVERLAY`), translúcidas y **no táctiles** (`FLAG_NOT_TOUCHABLE`): las demás apps se siguen usando.
- Cada banda pide al compositor un *background blur* acotado a sus bounds (`Window.setBackgroundBlurRadius`, Android 12+). El drawable de fondo es casi invisible: solo define el recorte. No se usa `FLAG_BLUR_BEHIND` / `setBlurBehindRadius` (eso desenfoca **toda** la pantalla).
- El giroscopio reparte el radio: lado lejano más blur, lado cercano ~0. De frente, radio 0 y las bandas se ocultan: **el teléfono se ve completamente normal**.
- Hay un sesgo geométrico mínimo del layout (el cristal «gira» un poco). No se capturan píxeles para deformar la UI.
- Servicio en primer plano con notificación permanente y silenciosa.
- Los sensores se pausan al apagar la pantalla.

## Por qué se quitó la captura (0.1.2)

0.1.2 pedía `MediaProjection` (el diálogo de «grabar pantalla»). Eso es inaceptable para este uso: no vamos a grabar el teléfono para fingir un cristal.

`RenderEffect.createBlurEffect` solo desenfoca lo que **esta** ventana dibuja. No sirve para desenfocar el launcher debajo de un overlay.

La única API pública que desenfoca píxeles de **otras** ventanas es el blur cruzado del compositor.

## Qué fallaba en 0.1.0 y 0.1.1

- 0.1.0 pintó un velo claro (brillo).
- 0.1.1 pintó niebla mate oscura «por si el OEM no hace blur».
- En ambos casos los iconos no se desenfocaban: se tapaban. La foto de Héctor es **defocus real**.

0.1.3 **no** vuelve a pintar un velo. Si el compositor no ofrece blur cruzado, la capa no se enciende y la UI lo dice en español.

## Si `isCrossWindowBlurEnabled` es falso

CristalGiro no activa un efecto falso. Verás un aviso claro:

- El teléfono tiene el desenfoque entre ventanas desactivado (sistema, ahorro de batería, vídeo, o el fabricante).
- Sin eso no se puede desenfocar el launcher ni otras apps con APIs públicas.
- No vamos a pedir captura de pantalla.

En Pixel / AOSP, si el hardware lo soporta (`ro.surface_flinger.supports_background_blur`):

**Ajustes → Sistema → Opciones de desarrollador → Representación acelerada por hardware → Permitir desenfoques a nivel de ventana.**

Si no aparece esa opción, el aparato no declara soporte. En muchas capas (Xiaomi / HyperOS, Huawei, Oppo, Vivo, Samsung) el interruptor no existe o no aplica a overlays de apps. CristalGiro no elude esa política.

## Compilar

Necesitas JDK 17+ y Android SDK (plataforma 35 y build-tools 35).

```bash
# Linux / macOS
export ANDROID_HOME=/ruta/al/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug

# El APK queda en:
# app/build/outputs/apk/debug/app-debug.apk
```

En Windows (PowerShell), separa órdenes con `;`:

```powershell
$env:ANDROID_HOME = "C:\Users\TU_USUARIO\AppData\Local\Android\Sdk"
Set-Content -Path local.properties -Value "sdk.dir=$ANDROID_HOME"
.\gradlew.bat assembleDebug
```

Instalar en un teléfono con depuración USB:

```bash
./gradlew installDebug
```

## Cómo activarlo en el teléfono

1. Instala el APK (depuración USB o sideload).
2. Abre CristalGiro.
3. Concede **mostrar sobre otras apps** si te lo pide.
4. En Android 13+ acepta el aviso silencioso de la notificación del servicio.
5. Si la app dice que el blur cruzado está apagado, enciéndelo (opciones de desarrollador, arriba) o usa un aparato/emulador que lo soporte. El emulador Android 12+ AOSP suele tenerlo.
6. Pulsa **Activar**. Sal a la pantalla de inicio. De frente no cambia nada. Gira el teléfono en X hacia la derecha: la **izquierda** se pone un poco blanda. Hacia la izquierda: al revés. Al volver de frente se quita.

Revocar: **Desactivar** en la app o en la notificación, o quitar la superposición en Ajustes.

## Permisos

| Permiso | Para qué |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Colocar las bandas encima. No intercepta toques. |
| Notificación (Android 13+) | Aviso silencioso del servicio. No es marketing. |
| Servicio en primer plano `specialUse` | Mantener las bandas mientras usas otras apps. |

No hay `MediaProjection`. No hay permiso de grabación.

## Límites (honesto)

- **Sin root.** Dependemos de que el compositor tenga *cross-window blur* activo (`WindowManager.isCrossWindowBlurEnabled`).
- **Android 11 e inferior:** no existe la API. La app lo dice y no finge.
- **Overlays `TYPE_APPLICATION_OVERLAY`:** hace falta ventana **flotante + translúcida** y un drawable de fondo que recorte el blur. Algunos OEM no aplican backdrop blur a overlays de terceros aunque el flag global esté en true.
- **Ahorro de batería** o reproducción de vídeo pueden apagar el blur en runtime; la capa se apaga, no se sustituye por niebla.
- Algunos equipos limitan cuántas overlays se pueden crear; si el sistema rechaza una banda, esa franja no aparece.
- Gasta GPU: radios altos (>150 px) los evitamos (AOSP recomienda no pasar de ~80–150).
- El esquema de la app **no** es el efecto real: solo muestra qué bandas pedirían radio.

CristalGiro no elude esas políticas y no usa captura de pantalla.

## Privacidad

- Sin red, sin anuncios, sin analítica.
- Solo guarda en el teléfono intensidad, suavidad, modo y si pediste activar la capa.
- No se incluyen secretos ni claves en el repositorio.

## Licencia de uso

Código para uso personal. Compílalo e instálalo tú; no se distribuye un binario firmado de producción en este repo.
