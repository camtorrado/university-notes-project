package com.notas.gui;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.model.ResultadoCarga;
import com.notas.service.GestionService;
import com.notas.service.GestionService.Base;
import com.notas.service.GestionService.EstadoSalida;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Foto de los archivos de /data en un momento dado, ya cruzada, para que todas las secciones
// pinten lo mismo. Se vuelve a construir despues de cada cambio.
record Datos(Base base, List<PromedioAlumno> ranking, List<String> advertencias,
             Map<String, Alumno> alumnosPorId, Map<String, Curso> cursosPorId,
             Map<String, Integer> horasPorAlumno, Map<String, Integer> reprobadosPorAlumno,
             Map<String, PromedioAlumno> promedioPorAlumno, long alumnosSinNotas,
             EstadoSalida estadoSalida) {

    static Datos de(Base base, ResultadoCarga<PromedioAlumno> ranking, EstadoSalida estadoSalida) {
        Map<String, Curso> cursosPorId = GestionService.mapPorId(base.cursos(), Curso::id);
        return new Datos(base, ranking.registros(), ranking.errores(),
            GestionService.mapPorId(base.alumnos(), Alumno::id), cursosPorId,
            GestionService.calcularHorasPorAlumno(cursosPorId, base.notas()),
            GestionService.contarReprobadosPorAlumno(base.notas()),
            ranking.registros().stream().collect(Collectors.toMap(PromedioAlumno::getId, Function.identity())),
            GestionService.contarSinNotas(base, ranking.registros()),
            estadoSalida);
    }

    static Datos vacio() {
        return de(new Base(List.of(), List.of(), List.of(), List.of()),
            new ResultadoCarga<>(List.of(), List.of()), EstadoSalida.NO_GENERADA);
    }

    List<Alumno> alumnos() {
        return base.alumnos();
    }

    List<Curso> cursos() {
        return base.cursos();
    }

    List<NotaCurso> notas() {
        return base.notas();
    }

    List<NotaCurso> notasDeAlumno(String alumnoId) {
        return base.notas().stream().filter(n -> n.alumnoId().equals(alumnoId)).toList();
    }

    List<NotaCurso> notasDeCurso(String cursoId) {
        return base.notas().stream().filter(n -> n.cursoId().equals(cursoId)).toList();
    }
}
