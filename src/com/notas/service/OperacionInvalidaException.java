package com.notas.service;

// Regla de negocio incumplida (nombre duplicado, ID inexistente, nota fuera de rango...).
// El mensaje esta pensado para mostrarse tal cual al usuario, en consola o en la interfaz grafica.
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
