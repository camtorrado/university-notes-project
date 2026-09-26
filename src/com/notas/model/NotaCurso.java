package com.notas.model;

// Los creditos no se guardan aqui, se resuelven contra Curso por cursoId (un solo lugar de verdad).
public record NotaCurso(String alumnoId, String cursoId, double nota) {
}
