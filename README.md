# CristalGiro

App Android (Kotlin) que pone **todo el teléfono** bajo un cristal grueso tipo Duo: desenfoque suave en esquinas o en un lado, que se mueve con el giroscopio. No es un filtro dentro de una sola pantalla: es una capa del sistema (overlay) que deja pasar los toques.

Paquete: `com.soyache.blurgiro` · versión `0.1.0` (versionCode 1).

Sin anuncios, sin rastreo y **sin root**.

## Qué hace

- Capa a pantalla completa, transparente y **no táctil** (`FLAG_NOT_TOUCHABLE`): las demás apps se siguen usando.
- El desenfoque **no es uniforme**. Al inclinar el aparato, un borde o las esquinas se van de foco y el lado opuesto permanece más nítido (profundidad de campo / cristal grueso).
- Dos modos: **solo esquinas** y **lado direccional**.
- Intensidad y suavidad del seguimiento configurables.
- Servicio en primer plano con notificación permanente y silenciosa mientras está activo.
- Los sensores se pausan al apagar la pantalla.

La capa combina:

1. Un sombreador (AGSL en Android 13+, degradados en versiones anteriores) con escarcha, viñeta y brillo especular.
2. Manchas en esquinas o en un lado que piden *blur* al compositor (`setBlurBehindRadius`, Android 12+) cuando el fabricante lo permite.

**No captura la pantalla** y no lee el contenido de otras apps. Si el compositor no ofrece desenfoque cruzado, verás sobre todo el velo de cristal (sigue reaccionando al giro).

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

## Límites de los fabricantes (OEM)

Esto **no requiere root**, y por eso depende de lo que Android y cada marca dejen hacer a una app normal:

- **Xiaomi / HyperOS, Huawei, Oppo, Vivo, Samsung**, etc. a veces bloquean ventanas emergentes, matan servicios en segundo plano o desactivan el desenfoque entre ventanas.
- El *blur* real del compositor puede estar apagado (opción de desarrollador «desenfoque entre ventanas» o política de la marca). En ese caso el efecto se ve más como cristal/escarcha que como un desenfoque óptico del fondo.
- Algunos equipos limitan cuántas overlays se pueden crear a la vez.
- Ahorro de batería agresivo puede detener el servicio: excluye CristalGiro de las restricciones de inicio automático si quieres que siga activo.

CristalGiro no elude esas políticas.

## Privacidad

- Sin red, sin anuncios, sin analítica.
- Solo guarda en el teléfono intensidad, suavidad, modo y si pediste activar la capa.
- No se incluyen secretos ni claves en el repositorio.

## Licencia de uso

Código para uso personal. Compílalo e instálalo tú; no se distribuye un binario firmado de producción en este repo.
