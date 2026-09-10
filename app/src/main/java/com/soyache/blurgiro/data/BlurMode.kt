package com.soyache.blurgiro.data

enum class BlurMode {
    /** Desenfoque denso en las esquinas; el centro permanece más nítido. */
    CORNERS,

    /** Un lado (el de la inclinación) se va de foco; el opuesto se mantiene claro. */
    DIRECTIONAL,
    ;

    companion object {
        fun fromOrdinal(value: Int): BlurMode =
            entries.getOrElse(value) { CORNERS }
    }
}
