package com.notas.model;

// Unica fuente de creditos y horas: editar un curso actualiza el promedio y la carga horaria
// de todos sus alumnos matriculados.
public record Curso(String id, String nombre, int creditos, int horasSemanales) {
}
