package com.soyache.blurgiro.data

enum class BlurMode {
    /** Desenfoque en las esquinas que se alejan; el centro permanece más nítido. */
    CORNERS,

    /** El lado que se aleja (eje X) se va de foco; el cercano se mantiene claro. */
    DIRECTIONAL,
    ;

    companion object {
        fun fromOrdinal(value: Int): BlurMode =
            entries.getOrElse(value) { DIRECTIONAL }
    }
}
