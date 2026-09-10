package com.soyache.blurgiro.effect

/**
 * Textos en español cuando el compositor no da blur cruzado a terceros.
 *
 * El interruptor «Permitir desenfoques a nivel de ventana» de Opciones de
 * desarrollador **solo existe** si el OEM compiló SurfaceFlinger con
 * `ro.surface_flinger.supports_background_blur=1`. Si no aparece, no hay
 * nada que habilitar: el aparato no soporta window blur para apps de
 * terceros. Estos textos no mandan a ese menú.
 */
object OemBlurCopy {

    fun apiUnsupportedTitle(): String = "Hace falta Android 12 o superior"

    fun apiUnsupportedBody(): String =
        "El desenfoque de otras ventanas (Window.setBackgroundBlurRadius) solo existe " +
            "desde Android 12. En este aparato no hay API pública para desenfocar el launcher " +
            "sin capturar la pantalla, y CristalGiro se niega a grabar. Tampoco pintamos un velo."

    fun oemUnsupportedTitle(manufacturer: String): String {
        val brand = brandLabel(manufacturer)
        return if (brand != null) {
            "$brand no activó el desenfoque del sistema para apps de terceros"
        } else {
            "El fabricante no activó el desenfoque del sistema para apps de terceros"
        }
    }

    fun oemUnsupportedBody(manufacturer: String, sdkInt: Int = 0): String {
        val intro = oemIntro(manufacturer)
        val release = androidRelease(sdkInt)
        val versionNote = if (release != null) {
            " Este aparato reporta $release (API $sdkInt)."
        } else {
            ""
        }
        return intro + versionNote +
            "\n\nCristalGiro no puede desenfocar el launcher ni otras apps sin capturar la " +
            "pantalla, y no vamos a pedir captura. Tampoco pintamos un velo ni niebla falsa: " +
            "sin blur del compositor no hay cristal."
    }

    internal fun androidRelease(sdkInt: Int): String? = when {
        sdkInt <= 30 -> null
        sdkInt == 31 -> "Android 12"
        sdkInt == 32 -> "Android 12L"
        sdkInt == 33 -> "Android 13"
        sdkInt == 34 -> "Android 14"
        sdkInt == 35 -> "Android 15"
        sdkInt == 36 -> "Android 16"
        else -> "API $sdkInt"
    }

    internal fun brandKey(manufacturer: String): String {
        val m = manufacturer.trim().lowercase()
        return when {
            m.contains("samsung") -> "samsung"
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> "xiaomi"
            m.contains("huawei") -> "huawei"
            m.contains("honor") -> "honor"
            m.contains("oppo") -> "oppo"
            m.contains("vivo") -> "vivo"
            m.contains("realme") -> "realme"
            m.contains("oneplus") || m.contains("one plus") -> "oneplus"
            m.contains("motorola") || m == "moto" -> "motorola"
            m.contains("google") || m.contains("pixel") -> "google"
            m.contains("nothing") -> "nothing"
            m.contains("sony") -> "sony"
            m.contains("asus") -> "asus"
            else -> "generic"
        }
    }

    private fun brandLabel(manufacturer: String): String? = when (brandKey(manufacturer)) {
        "samsung" -> "Samsung"
        "xiaomi" -> "Xiaomi"
        "huawei" -> "Huawei"
        "honor" -> "Honor"
        "oppo" -> "OPPO"
        "vivo" -> "vivo"
        "realme" -> "realme"
        "oneplus" -> "OnePlus"
        "motorola" -> "Motorola"
        "google" -> "Google"
        "nothing" -> "Nothing"
        "sony" -> "Sony"
        "asus" -> "ASUS"
        else -> null
    }

    private fun oemIntro(manufacturer: String): String = when (brandKey(manufacturer)) {
        "samsung" ->
            "En este teléfono Samsung, WindowManager.isCrossWindowBlurEnabled es falso. " +
                "One UI puede tener desenfoques propios, pero Samsung no habilita el blur " +
                "de SurfaceFlinger para overlays de terceros. No es un menú que falte en " +
                "la app: el fabricante no abrió esa API."
        "xiaomi" ->
            "En este Xiaomi / HyperOS, WindowManager.isCrossWindowBlurEnabled es falso. " +
                "MIUI puede desenfocar su propia interfaz y aun así dejar el blur cruzado " +
                "público apagado para apps como CristalGiro."
        "huawei" ->
            "En este Huawei, WindowManager.isCrossWindowBlurEnabled es falso. EMUI / Harmony " +
                "no suele exponer blur de ventana a aplicaciones de terceros."
        "honor" ->
            "En este Honor, WindowManager.isCrossWindowBlurEnabled es falso. MagicOS no suele " +
                "exponer blur de ventana a aplicaciones de terceros."
        "oppo" ->
            "En este OPPO, WindowManager.isCrossWindowBlurEnabled es falso. ColorOS reserva " +
                "sus desenfoques para el sistema y no los da a overlays de terceros."
        "vivo" ->
            "En este vivo, WindowManager.isCrossWindowBlurEnabled es falso. Funtouch / OriginOS " +
                "no suele abrir el blur cruzado a apps de terceros."
        "realme" ->
            "En este realme, WindowManager.isCrossWindowBlurEnabled es falso. realme UI no suele " +
                "abrir el blur cruzado a apps de terceros."
        "oneplus" ->
            "En este OnePlus, WindowManager.isCrossWindowBlurEnabled es falso. OxygenOS / ColorOS " +
                "pueden tener desenfoques propios y aun así no darlos a overlays de terceros."
        "motorola" ->
            "En este Motorola, WindowManager.isCrossWindowBlurEnabled es falso. El fabricante no " +
                "habilitó blur de ventana para aplicaciones de terceros en este aparato."
        "google" ->
            "En este Pixel, WindowManager.isCrossWindowBlurEnabled es falso: el compositor no " +
                "aplica blur cruzado a apps de terceros ahora mismo. CristalGiro no puede " +
                "desenfocar el launcher sin esa API."
        "nothing" ->
            "En este Nothing, WindowManager.isCrossWindowBlurEnabled es falso. El fabricante no " +
                "habilitó blur de ventana para aplicaciones de terceros en este aparato."
        "sony" ->
            "En este Sony, WindowManager.isCrossWindowBlurEnabled es falso. El fabricante no " +
                "habilitó blur de ventana para aplicaciones de terceros en este aparato."
        "asus" ->
            "En este ASUS, WindowManager.isCrossWindowBlurEnabled es falso. El fabricante no " +
                "habilitó blur de ventana para aplicaciones de terceros en este aparato."
        else ->
            "En este teléfono el fabricante no habilitó el desenfoque del sistema para " +
                "aplicaciones de terceros (WindowManager.isCrossWindowBlurEnabled es falso). " +
                "Aunque la interfaz del fabricante tenga desenfoques propios, eso no abre la " +
                "API pública a overlays como CristalGiro."
    }
}
