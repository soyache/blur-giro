package com.soyache.blurgiro.effect

/**
 * Paleta del fallback: niebla mate / cristal esmerilado.
 * Nunca blanco ni cian claro: ese era el «brillo raro» de 0.1.0.
 */
object GlassLook {
    const val HAZE_R = 0.16f
    const val HAZE_G = 0.17f
    const val HAZE_B = 0.20f

    const val HAZE_R_BYTE = 41
    const val HAZE_G_BYTE = 44
    const val HAZE_B_BYTE = 51

    /** Alpha máxima del velo SRC_OVER sobre otras apps. Por encima se lee como mancha. */
    const val MAX_MIST_ALPHA = 0.26f

    /** Si el compositor sí desenfoca, la niebla se queda como grano de escarcha. */
    const val MIST_WHEN_BLUR_LIVE = 0.38f
}
