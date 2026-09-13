package com.soyache.blurgiro;

/** Spanish setup / failure copy. Never invent a painted mist when capture fails. */
final class SetupMessages {
    private SetupMessages() {}

    static String steps() {
        return "Pasos: 1) instala e inicia Shizuku  2) autoriza  3) activa accesibilidad. "
                +"No usamos el diálogo de grabar pantalla.";
    }

    static String needShizuku() {
        return "Primero termina Shizuku (iniciar + autorizar). "+steps();
    }

    static String captureFailed(String detail) {
        String extra=detail==null||detail.trim().isEmpty()?"":"\n\nDetalle: "+detail;
        return "No pude capturar la pantalla. No pinto una niebla falsa."+extra+"\n\n"+steps();
    }
}
