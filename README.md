# CristalGiro

App Android (Kotlin) que pone **todo el teléfono** bajo un cristal tipo Duo: de frente se ve normal; al girar en X el lado que se aleja se desenfoca un poco (los iconos se quedan, blandos) y la capa hace un warp de perspectiva suave.

Paquete: `com.soyache.blurgiro` · versión `0.1.2` (versionCode 3).

Sin anuncios, sin rastreo y **sin root**.

## Qué hace

- Capa a pantalla completa, transparente y **no táctil** (`FLAG_NOT_TOUCHABLE`): las demás apps se siguen usando.
- **De frente / plano:** identidad. Cero blur, cero warp. Se ve el teléfono normal.
- **Giro en X** (izquierda–derecha): el lado que se alejó recibe un desenfoque suave del *contenido real*; el lado cercano sigue legible. La imagen también se achica un poco en el lado lejano (parece que el cristal gira).
- Un poco de Y se admite, con menos peso. Prioridad al eje X, como en la foto de referencia.
- Intensidad «un poco»: no es un lavado.
- Servicio en primer plano con notificación permanente y silenciosa.
- Los sensores se pausan al apagar la pantalla.

## Por qué 0.1.0 y 0.1.1 no coincidían con la foto

Un overlay de app (`TYPE_APPLICATION_OVERLAY`) **casi nunca** puede pedirle al compositor que desenfogue los píxeles de otras apps. `Window.setBackgroundBlurRadius` / *cross-window blur* o no entra, o desenfoca toda la pantalla.

0.1.0 pintó un velo claro (brillo). 0.1.1 pintó niebla mate oscura. En ambos casos los iconos no se desenfocaban: se tapaban. La foto de Héctor es **defocus real** (manchas de color blandas, WhatsApp/Gmail/ChatGPT siguen reconociéndose al otro lado). Eso no se puede fingir con tinte.

## Cómo se hace ahora (0.1.2)

Si el compositor no puede desenfocar otras apps bajo un overlay —el caso normal en stock Android—, CristalGiro usa **MediaProjection con tu permiso explícito**:

1. Tú pulsas **Activar**.
2. La app explica en español qué va a pasar.
3. Android muestra el diálogo del sistema de captura de pantalla. **No hay captura silenciosa.**
4. Mientras la capa está encendida, se toma la pantalla a baja resolución.
5. De frente no se dibuja nada (ves la pantalla real).
6. Al inclinar, se aplica un blur direccional (pirámide nítida / media / fuerte) + un trapecio suave, y eso se pinta en el overlay.
7. Para no desenfocar el desenfoque, mientras el efecto está visible se deja de aceptar frames nuevos. Al volver de frente se refresca.

No se guarda el vídeo. No se sube a internet. Al desactivar se corta la proyección.

### Compromisos de este camino

- Android te muestra el icono / aviso de «grabando pantalla» (lo exige el sistema).
- Mientras inclinas ves el **último fotograma limpio** procesado. Si cambias de app inclinado, endereza un momento para refrescar.
- Apps `FLAG_SECURE` (banca, DRM, Netflix, etc.) salen en negro en la captura.
- Algunos OEM recortan MediaProjection o matan el servicio con el ahorro de batería.
- Gasta un poco más de batería que un overlay vacío.

No hay fallback de niebla ni de brillo: si no concedes la captura, la capa no se enciende.

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
3. Concede **mostrar sobre otras apps** (`SYSTEM_ALERT_WINDOW`) si te lo pide.
4. En Android 13+ acepta el aviso silencioso de la notificación del servicio.
5. Pulsa **Activar**. Lee el texto: la app necesita ver la pantalla para desenfocar los iconos de verdad.
6. En el diálogo de Android, acepta la captura (a veces dice «iniciar ahora» / «una app quiere grabar»).
7. Sal a la pantalla de inicio. De frente no cambia nada. Gira el teléfono en X hacia la derecha: la **izquierda** se pone un poco blanda. Hacia la izquierda: al revés. Al volver de frente se quita.

Revocar:

- **Desactivar** en la app o en la notificación.
- Quitar superposición: **Ajustes → Aplicaciones → CristalGiro → Mostrar encima de otras aplicaciones**.
- La captura se corta al desactivar; no queda un permiso permanente de grabación (el sistema la vuelve a pedir la próxima vez).

## Permisos

| Permiso | Para qué |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | Dibujar la capa encima. No intercepta toques. |
| Captura de pantalla (`MediaProjection`) | Ver los píxeles de debajo **solo** para el efecto. Diálogo del sistema, cada vez que activas. |
| Notificación (Android 13+) | Aviso silencioso del servicio. No es marketing. |
| Servicio en primer plano `mediaProjection` + `specialUse` | Exigencia de Android 14+ para mantener la captura y la capa. |

## Límites de Android y de los fabricantes (OEM)

- **Sin root.** No se puede leer el framebuffer de otras apps sin captura o sin APIs de compositor que los OEM suelen negar a overlays.
- **Pixel / AOSP:** el diálogo de MediaProjection es el estándar. A veces hay un interruptor de desenfoque entre ventanas en opciones de desarrollador; CristalGiro **ya no depende** de él.
- **Xiaomi / HyperOS, Huawei, Oppo, Vivo, Samsung:** pueden pedir permisos extra de «mostrar sobre otras apps», «inicio en segundo plano» o limitar la grabación de pantalla. Si la capa no aparece o la captura se corta, excluye CristalGiro del ahorro de batería.
- **Android 11 e inferior:** MediaProjection existe; el look es el mismo. No hay blur cruzado útil para overlays.
- **Rotación / multi-ventana:** la captura se recrea; puede haber un frame en negro un instante.
- Si el sistema te quita la proyección (el aviso de grabar), la capa se apaga. Vuelve a pulsar Activar.

CristalGiro no elude esas políticas.

## Privacidad

- Sin red, sin anuncios, sin analítica.
- La captura vive en memoria mientras el servicio corre. No se escribe a disco ni se envía.
- Solo se guardan en el teléfono intensidad, suavidad, modo y si pediste activar la capa.
- No se incluyen secretos ni claves en el repositorio.

## Licencia de uso

Código para uso personal. Compílalo e instálalo tú; no se distribuye un binario firmado de producción en este repo.
