package com.notas.service;

import com.notas.model.Alumno;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromedioService {

    // Nota minima (escala 0.0 - 5.0) para figurar en el cuadro de honor.
    public static final double UMBRAL_HONOR = 4.5;

    // Cruza cada nota con el alumno por ID y acumula el promedio ponderado por creditos.
    public List<PromedioAlumno> calcularRanking(Map<String, Alumno> alumnos, List<NotaCurso> notas) {
        Map<String, PromedioAlumno> acumulado = new LinkedHashMap<>();

        for (NotaCurso nota : notas) {
            Alumno alumno = alumnos.get(nota.alumnoId());
            if (alumno == null) continue; // nota referencia un alumno que no esta en el catalogo

            PromedioAlumno promedio = acumulado.computeIfAbsent(
                alumno.id(), id -> new PromedioAlumno(alumno.id(), alumno.nombreCompleto())
            );
            promedio.agregarNota(nota.nota(), nota.creditos());
        }

        return acumulado.values().stream()
            .sorted(Comparator.comparingDouble(PromedioAlumno::getPromedio).reversed())
            .collect(Collectors.toList());
    }

    public boolean esCuadroDeHonor(PromedioAlumno promedio) {
        return promedio.getPromedio() >= UMBRAL_HONOR;
    }
}
