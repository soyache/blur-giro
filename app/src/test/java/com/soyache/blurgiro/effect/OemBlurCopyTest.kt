package com.soyache.blurgiro.effect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OemBlurCopyTest {

    private val manufacturers = listOf(
        "samsung",
        "Samsung",
        "Xiaomi",
        "Redmi",
        "POCO",
        "HUAWEI",
        "HONOR",
        "OPPO",
        "vivo",
        "realme",
        "OnePlus",
        "motorola",
        "Google",
        "Nothing",
        "Sony",
        "asus",
        "Fairphone",
        "",
        "  ",
    )

    @Test
    fun neverSendsUserToDeveloperToggle() {
        val forbidden = listOf(
            "opciones de desarrollador",
            "opción de desarrollador",
            "permitir desenfoques",
            "desenfoques a nivel de ventana",
            "abrir opciones",
            "enciéndelo",
            "enciendela",
            "habilita una opción",
            "habilite una opción",
            "representation acelerada",
        )
        val texts = manufacturers.flatMap { m ->
            listOf(
                OemBlurCopy.oemUnsupportedTitle(m),
                OemBlurCopy.oemUnsupportedBody(m, 34),
            )
        } + listOf(
            OemBlurCopy.apiUnsupportedTitle(),
            OemBlurCopy.apiUnsupportedBody(),
        )
        for (text in texts) {
            val lower = text.lowercase()
            for (phrase in forbidden) {
                assertFalse("«$phrase» no debe aparecer en: $text", lower.contains(phrase))
            }
        }
    }

    @Test
    fun oemBodyRefusesCaptureAndFakeVeil() {
        for (m in manufacturers) {
            val body = OemBlurCopy.oemUnsupportedBody(m, 35).lowercase()
            assertTrue("$m debe negar captura", body.contains("no vamos a pedir captura"))
            assertTrue("$m debe negar velo", body.contains("tampoco pintamos un velo"))
            assertTrue("$m debe hablar del launcher", body.contains("launcher"))
        }
    }

    @Test
    fun samsungMentionsOwnUiDoesNotOpenApi() {
        val body = OemBlurCopy.oemUnsupportedBody("samsung", 34)
        assertTrue(body.contains("Samsung"))
        assertTrue(body.contains("One UI"))
        assertTrue(OemBlurCopy.oemUnsupportedTitle("samsung").contains("Samsung"))
    }

    @Test
    fun xiaomiFamilySharesCopy() {
        assertTrue(OemBlurCopy.oemUnsupportedBody("Xiaomi", 33).contains("HyperOS"))
        assertTrue(OemBlurCopy.oemUnsupportedBody("Redmi", 33).contains("HyperOS"))
        assertTrue(OemBlurCopy.oemUnsupportedBody("POCO", 33).contains("HyperOS"))
        assertTrue(OemBlurCopy.oemUnsupportedTitle("xiaomi").contains("Xiaomi"))
    }

    @Test
    fun genericManufacturerStaysHonest() {
        val title = OemBlurCopy.oemUnsupportedTitle("Fairphone")
        val body = OemBlurCopy.oemUnsupportedBody("Fairphone", 34)
        assertTrue(title.contains("fabricante"))
        assertTrue(body.contains("isCrossWindowBlurEnabled"))
        assertTrue(body.contains("terceros"))
        assertTrue(body.contains("Android 14"))
    }

    @Test
    fun androidReleaseNamesKnownSdks() {
        assertTrue(OemBlurCopy.androidRelease(31) == "Android 12")
        assertTrue(OemBlurCopy.androidRelease(35) == "Android 15")
        assertTrue(OemBlurCopy.androidRelease(0) == null)
    }

    @Test
    fun android12ApiCopyRefusesCapture() {
        val body = OemBlurCopy.apiUnsupportedBody().lowercase()
        assertTrue(body.contains("android 12"))
        assertTrue(body.contains("se niega a grabar"))
        assertTrue(body.contains("velo"))
    }

    @Test
    fun brandKeysNormalizeAliases() {
        assertTrue(OemBlurCopy.brandKey("samsung") == "samsung")
        assertTrue(OemBlurCopy.brandKey("Redmi") == "xiaomi")
        assertTrue(OemBlurCopy.brandKey("Google") == "google")
        assertTrue(OemBlurCopy.brandKey("unknown-oem") == "generic")
    }
}
