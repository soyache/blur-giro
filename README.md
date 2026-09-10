# CristalGiro

App Android (Kotlin) que pone **todo el teléfono** bajo un cristal grueso tipo Duo: desenfoque suave en esquinas o en un lado, que se mueve con el giroscopio. No es un filtro dentro de una sola pantalla: es una capa del sistema (overlay) que deja pasar los toques.

Paquete: `com.soyache.blurgiro` · versión `0.1.1` (versionCode 2).

Sin anuncios, sin rastreo y **sin root**.

## Qué hace

- Capa a pantalla completa, transparente y **no táctil** (`FLAG_NOT_TOUCHABLE`): las demás apps se siguen usando.
- El desenfoque **no es uniforme**. Al inclinar el aparato, un borde o las esquinas se van de foco y el lado opuesto permanece más nítido (profundidad de campo / cristal grueso).
- En reposo (teléfono plano) el efecto se apaga: **no hay un velo blanco ni un brillo** a pantalla completa.
- Dos modos: **solo esquinas** y **lado direccional**.
- Intensidad y suavidad del seguimiento configurables.
- Servicio en primer plano con notificación permanente y silenciosa mientras está activo.
- Los sensores se pausan al apagar la pantalla.

La capa combina:

1. Parches en esquinas o bandas laterales que piden *background blur* al compositor (`Window.setBackgroundBlurRadius`, Android 12+) **dentro de esos recortes**. No se usa `FLAG_BLUR_BEHIND`: ese API desenfoca **toda** la pantalla y destrozaría el centro nítido.
2. Un sombreador (AGSL en Android 13+, degradados en versiones anteriores) con **niebla mate oscura** y grano, solo en la dirección de la inclinación. Es el fallback cuando el OEM no desenfoca detrás de un overlay.

**No captura la pantalla** y no lee el contenido de otras apps (no hay `MediaProjection`). Si el compositor no ofrece desenfoque cruzado, verás la niebla mate direccional — no un resplandor.

## Qué fallaba en 0.1.0

En un teléfono real el overlay se veía como «un brillo, todo raro». Causas:

- El fallback pintaba escarcha **cian/blanca**, highlights especulares y un `RenderEffect` que solo desenfocaba esa pintura (bloom).
- Las manchas de esquina tenían alpha alto y color claro. En reposo seguían visibles.
- `FLAG_BLUR_BEHIND` casi nunca entra en `TYPE_APPLICATION_OVERLAY`; y si entra, desenfoca la pantalla entera.

0.1.1 quita el velo claro, apaga el efecto en reposo y pide backdrop blur recortado cuando el sistema lo permite.

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
Set-Content -Path local.properties -Value "sdk.dir=$env:ANDROID_HOME"
.\gradlew.bat assembleDebug
```

Instalar en un teléfono con depuración USB:

```bash
./gradlew installDebug
```

## Permiso de superposición

Android llama al permiso `SYSTEM_ALERT_WINDOW` («mostrar sobre otras apps»). CristalGiro lo pide de forma explícita:

- Sirve **solo** para dibujar la capa de cristal encima de lo que haya en pantalla.
- No da acceso al contenido de otras apps, no hace capturas y no intercepta toques.
- Puedes revocarlo en **Ajustes → Aplicaciones → CristalGiro → Mostrar encima de otras aplicaciones** (el nombre exacto cambia según el fabricante).

Sin ese permiso la app no puede activar la capa. El botón **Activar** te lleva a la pantalla del sistema si falta.

En Android 13+ también se pide el permiso de notificaciones para el aviso silencioso del servicio. No se usa para marketing.

## Límites de Android y de los fabricantes (OEM)

Esto **no requiere root**, y por eso depende de lo que Android y cada marca dejen hacer a una app normal:

- **Desenfoque real del fondo** (Android 12+): hace falta que el compositor tenga *cross-window blur* activo (`WindowManager.isCrossWindowBlurEnabled`). En Pixel/AOSP a veces se enciende en **Opciones de desarrollador → Representación acelerada por hardware → Permitir desenfoques a nivel de ventana**.
- **`TYPE_APPLICATION_OVERLAY`**: muchas capas de fabricante (Xiaomi / HyperOS, Huawei, Oppo, Vivo, Samsung, etc.) no aplican backdrop blur a overlays de apps. En ese caso **no hay forma pública de desenfocar el wallpaper u otras apps sin capturar la pantalla**, que CristalGiro no hace.
- **Android 11 e inferior**: no existe la API de blur cruzado. Solo la niebla mate direccional.
- **Ahorro de batería** agresivo puede detener el servicio: excluye CristalGiro de las restricciones de inicio automático si quieres que siga activo.
- Algunos equipos limitan cuántas overlays se pueden crear a la vez; los parches se omiten si el sistema los rechaza y queda la niebla.

CristalGiro no elude esas políticas y no usa root ni captura de pantalla.

## Privacidad

- Sin red, sin anuncios, sin analítica.
- Solo guarda en el teléfono intensidad, suavidad, modo y si pediste activar la capa.
- No se incluyen secretos ni claves en el repositorio.

## Licencia de uso

Código para uso personal. Compílalo e instálalo tú; no se distribuye un binario firmado de producción en este repo.
