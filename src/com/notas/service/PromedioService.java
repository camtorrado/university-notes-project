package com.notas.service;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.model.ResultadoCarga;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromedioService {

    // Nota minima (escala 0.0 - 5.0) para figurar en el cuadro de honor.
    public static final double UMBRAL_HONOR = 4.5;

    // Nota minima (escala 0.0 - 5.0) para aplicar a beca.
    public static final double UMBRAL_BECA = 4.0;

    // Cruza notas con alumnos y cursos por ID. Una referencia rota no detiene el calculo,
    // solo queda como advertencia.
    public ResultadoCarga<PromedioAlumno> calcularRanking(
            Map<String, Alumno> alumnos, Map<String, Curso> cursos, List<NotaCurso> notas) {
        Map<String, PromedioAlumno> acumulado = new LinkedHashMap<>();
        List<String> advertencias = new ArrayList<>();

        for (NotaCurso nota : notas) {
            Alumno alumno = alumnos.get(nota.alumnoId());
            if (alumno == null) {
                advertencias.add("Nota con alumno inexistente: " + nota.alumnoId() + " (curso " + nota.cursoId() + ")");
                continue;
            }
            Curso curso = cursos.get(nota.cursoId());
            if (curso == null) {
                advertencias.add("Nota con curso inexistente: " + nota.cursoId() + " (alumno " + nota.alumnoId() + ")");
                continue;
            }

            PromedioAlumno promedio = acumulado.computeIfAbsent(
                alumno.id(), id -> new PromedioAlumno(alumno.id(), alumno.nombreCompleto())
            );
            promedio.agregarNota(nota.nota(), curso.creditos());
        }

        List<PromedioAlumno> ranking = acumulado.values().stream()
            .sorted(Comparator.comparingDouble(PromedioAlumno::getPromedio).reversed())
            .collect(Collectors.toList());

        return new ResultadoCarga<>(ranking, advertencias);
    }

    public boolean esCuadroDeHonor(PromedioAlumno promedio) {
        return promedio.getPromedio() >= UMBRAL_HONOR;
    }

    public boolean aplicaBeca(PromedioAlumno promedio) {
        return promedio.getPromedio() >= UMBRAL_BECA;
    }
}
