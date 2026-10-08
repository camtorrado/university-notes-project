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

public class PromedioService {

    // Nota minima (escala 0.0 - 5.0) para figurar en el cuadro de honor.
    public static final double UMBRAL_HONOR = 4.5;

    // Nota minima (escala 0.0 - 5.0) para aplicar a beca.
    public static final double UMBRAL_BECA = 4.0;

    // Nota minima para aprobar un curso. Solo informativa: no cambia beca ni cuadro de honor.
    public static final double NOTA_APROBATORIA = 3.0;

    // Mayor promedio primero; en empate, quien curso mas creditos y luego por nombre.
    private static final Comparator<PromedioAlumno> ORDEN_RANKING =
        Comparator.comparingDouble(PromedioAlumno::getPromedio).reversed()
            .thenComparing(Comparator.comparingInt(PromedioAlumno::getSumaCreditos).reversed())
            .thenComparing(PromedioAlumno::getNombreCompleto, String.CASE_INSENSITIVE_ORDER);

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

        List<PromedioAlumno> ranking = new ArrayList<>(acumulado.values());
        ranking.sort(ORDEN_RANKING);
        asignarPuestos(ranking);
        return new ResultadoCarga<>(ranking, advertencias);
    }

    // Ranking de competencia: con el mismo promedio comparten puesto y el siguiente salta (1, 2, 2, 4).
    private void asignarPuestos(List<PromedioAlumno> ranking) {
        for (int i = 0; i < ranking.size(); i++) {
            PromedioAlumno actual = ranking.get(i);
            boolean empataConAnterior = i > 0 && actual.getPromedio() == ranking.get(i - 1).getPromedio();
            actual.setPuesto(empataConAnterior ? ranking.get(i - 1).getPuesto() : i + 1);
        }
    }

    public static boolean esCuadroDeHonor(double promedio) {
        return promedio >= UMBRAL_HONOR;
    }

    public static boolean aplicaBeca(double promedio) {
        return promedio >= UMBRAL_BECA;
    }

    public static boolean estaAprobada(double nota) {
        return nota >= NOTA_APROBATORIA;
    }

    // Cuanto le falta al promedio para llegar al umbral (0 si ya lo alcanza).
    public static double faltaPara(double umbral, double promedio) {
        return Math.max(0.0, Math.round((umbral - promedio) * 100.0) / 100.0);
    }
}
