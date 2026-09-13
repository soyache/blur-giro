/*
 * Adapted from DuoFold-Android (https://github.com/jcx396905-gif/DuoFold-Android)
 * Copyright (c) 2026 jcx / jcx396905-gif
 * Licensed under the MIT License.
 *
 * CristalGiro: IDs estables en inglés; títulos y descripciones en español.
 */
package com.soyache.blurgiro.motion;

/** Persist stable IDs rather than UI positions so upgrades keep the selected effect. */
public enum EffectStyle {
    CLASSIC("classic", 0, "Cristal esmerilado · predeterminado",
            "De frente se ve nítido. Cuanto más inclinas, más blando y oscuro queda el lado que se aleja; al parar se mantiene el esmerilado."),
    EDGE("edge", 1, "Desvanecido en el borde",
            "El borde anclado sigue claro; el blur y la sombra se concentran en el lado que se abre. Al parar se mantiene el degradado."),
    SOFT("soft", 2, "Profundidad suave",
            "Muestreo más suave y sombra más ligera; al parar queda un cristal leve."),
    SETTLE("settle", 3, "Al reposar se aclara",
            "Se desenfoca al mover. Tras medio segundo quieto se aclara; la perspectiva se queda. Al mover otra vez vuelve el blur."),
    CLEAR("clear", 4, "Proyección nítida",
            "Mantiene el pliegue en perspectiva, sin esmerilado ni sombra extra."),
    BOKEH("bokeh", 5, "Bokeh de lente",
            "Perspectiva de cámara independiente: cerca nítido, lejos se abre. La intensidad controla el diafragma.");

    public final String id, title, description;
    public final int shaderId;
    EffectStyle(String id,int shaderId,String title,String description) {
        this.id=id;this.shaderId=shaderId;this.title=title;this.description=description;
    }
    public static EffectStyle fromId(String id) {
        for(EffectStyle style:values())if(style.id.equals(id))return style;
        return CLASSIC;
    }
}
