package com.notas.service;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GeneradorArchivosService {

    private static final List<String> NOMBRES = List.of(
        "Carlos", "Maria", "Andres", "Laura", "Juan", "Camila", "Diego", "Valentina",
        "Santiago", "Isabella", "Felipe", "Daniela", "Alejandro", "Gabriela", "Sebastian"
    );
    private static final List<String> APELLIDOS = List.of(
        "Perez", "Gomez", "Rodriguez", "Martinez", "Lopez", "Garcia", "Hernandez",
        "Torres", "Ramirez", "Vargas", "Castro", "Ortiz", "Rojas", "Molina"
    );
    private static final List<String> NOMBRES_CURSOS = List.of(
        "Calculo", "Algebra Lineal", "Programacion", "Estructuras de Datos", "Fisica",
        "Bases de Datos", "Redes", "Ingles", "Estadistica", "Arquitectura de Software"
    );
    private static final int[] CREDITOS_POSIBLES = {1, 2, 3, 4};
    private static final int[] HORAS_POSIBLES = {1, 2, 3, 4};

    private final Random random = new Random();

    // Crea cursos.csv, alumnos.csv y notas.txt pseudoaleatorios pero coherentes entre si.
    public void generarEjemplos(Path alumnosPath, Path cursosPath, Path notasPath,
                                 int cantidadAlumnos, int cantidadCursos) throws IOException {
        List<Curso> cursos = generarCursos(cantidadCursos);
        List<Alumno> alumnos = generarAlumnos(cantidadAlumnos);

        StringBuilder notasSb = new StringBuilder();
        for (Alumno alumno : alumnos) {
            for (NotaCurso nota : generarNotasAleatorias(alumno.id(), cursos)) {
                notasSb.append(nota.alumnoId()).append(';').append(nota.cursoId()).append(';')
                       .append(String.format("%.1f", nota.nota()))
                       .append(System.lineSeparator());
            }
        }

        guardarCursos(cursosPath, cursos);
        guardarAlumnos(alumnosPath, alumnos);
        Files.writeString(notasPath, notasSb.toString());
    }

    // Entre 3 y 6 notas pseudoaleatorias para un alumno, sin repetir curso (un alumno no puede
    // tener dos notas en el mismo curso).
    public List<NotaCurso> generarNotasAleatorias(String alumnoId, List<Curso> cursos) {
        int cantidadNotas = Math.min(cursos.size(), 3 + random.nextInt(4));
        List<Curso> cursosBarajados = new ArrayList<>(cursos);
        Collections.shuffle(cursosBarajados, random);

        List<NotaCurso> notas = new ArrayList<>();
        for (int c = 0; c < cantidadNotas; c++) {
            notas.add(new NotaCurso(alumnoId, cursosBarajados.get(c).id(), notaAleatoria()));
        }
        return notas;
    }

    // Una nota pseudoaleatoria en un curso puntual para cada alumno de la lista dada.
    public List<NotaCurso> generarNotasAleatoriasParaCurso(String cursoId, List<Alumno> alumnos) {
        List<NotaCurso> notas = new ArrayList<>();
        for (Alumno alumno : alumnos) {
            notas.add(new NotaCurso(alumno.id(), cursoId, notaAleatoria()));
        }
        return notas;
    }

    private List<Curso> generarCursos(int cantidad) {
        List<Curso> cursos = new ArrayList<>();
        Set<String> nombresUsados = new HashSet<>();
        for (int i = 1; i <= cantidad; i++) {
            String nombre = nombreCursoUnico(nombresUsados);
            nombresUsados.add(nombre.toLowerCase());
            int creditos = CREDITOS_POSIBLES[random.nextInt(CREDITOS_POSIBLES.length)];
            int horas = HORAS_POSIBLES[random.nextInt(HORAS_POSIBLES.length)];
            cursos.add(new Curso("C" + i, nombre, creditos, horas));
        }
        return cursos;
    }

    // Prueba los nombres base sin repetir; si se agotan, agrega un sufijo numerico.
    private String nombreCursoUnico(Set<String> nombresUsados) {
        List<String> disponibles = new ArrayList<>(NOMBRES_CURSOS);
        Collections.shuffle(disponibles, random);
        for (String candidato : disponibles) {
            if (!nombresUsados.contains(candidato.toLowerCase())) return candidato;
        }
        String base = NOMBRES_CURSOS.get(random.nextInt(NOMBRES_CURSOS.size()));
        int sufijo = 2;
        while (nombresUsados.contains((base + " " + sufijo).toLowerCase())) sufijo++;
        return base + " " + sufijo;
    }

    private List<Alumno> generarAlumnos(int cantidad) {
        List<Alumno> alumnos = new ArrayList<>();
        Set<String> nombresUsados = new HashSet<>();
        for (int i = 1; i <= cantidad; i++) {
            String nombre = nombreAlumnoUnico(nombresUsados);
            nombresUsados.add(nombre.toLowerCase());
            alumnos.add(new Alumno("A" + i, nombre));
        }
        return alumnos;
    }

    // Reintenta combinaciones nombre+apellido; si se agotan, agrega un sufijo numerico.
    private String nombreAlumnoUnico(Set<String> nombresUsados) {
        for (int intento = 0; intento < 50; intento++) {
            String candidato = nombreAleatorio();
            if (!nombresUsados.contains(candidato.toLowerCase())) return candidato;
        }
        String base = nombreAleatorio();
        int sufijo = 2;
        while (nombresUsados.contains((base + " " + sufijo).toLowerCase())) sufijo++;
        return base + " " + sufijo;
    }

    private void guardarCursos(Path path, List<Curso> cursos) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Curso c : cursos) {
            sb.append(c.id()).append(';').append(c.nombre()).append(';')
              .append(c.creditos()).append("_creditos").append(';')
              .append(c.horasSemanales()).append("_horas").append(System.lineSeparator());
        }
        Files.writeString(path, sb.toString());
    }

    private void guardarAlumnos(Path path, List<Alumno> alumnos) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Alumno a : alumnos) {
            sb.append(a.id()).append(';').append(a.nombreCompleto()).append(System.lineSeparator());
        }
        Files.writeString(path, sb.toString());
    }

    private String nombreAleatorio() {
        String nombre = NOMBRES.get(random.nextInt(NOMBRES.size()));
        String apellido = APELLIDOS.get(random.nextInt(APELLIDOS.size()));
        return nombre + " " + apellido;
    }

    // Nota entre 2.0 y 5.0, redondeada a un decimal, para simular un rango realista.
    private double notaAleatoria() {
        double nota = 2.0 + random.nextDouble() * 3.0;
        return Math.round(nota * 10.0) / 10.0;
    }

    // Formato de salida por linea: N_Puesto;ID;NombreCompleto;X.X_Prom;SI_Beca|NO_Beca;
    // SI_Honor|NO_Honor;M_HorasSemanales  ej: 1_Puesto;A1;Carlos Perez;4.6_Prom;SI_Beca;SI_Honor;5_HorasSemanales
    public void exportar(Path salidaPath, List<PromedioAlumno> ranking,
                          Map<String, Integer> horasPorAlumno) throws IOException {
        StringBuilder sb = new StringBuilder();
        int puesto = 0;
        for (PromedioAlumno p : ranking) {
            puesto++;
            String beca = p.getPromedio() >= PromedioService.UMBRAL_BECA ? "SI_Beca" : "NO_Beca";
            String honor = p.getPromedio() >= PromedioService.UMBRAL_HONOR ? "SI_Honor" : "NO_Honor";
            int horas = horasPorAlumno.getOrDefault(p.getId(), 0);
            sb.append(puesto).append("_Puesto").append(';')
              .append(p.getId()).append(';')
              .append(p.getNombreCompleto()).append(';')
              .append(String.format("%.1f", p.getPromedio())).append("_Prom").append(';')
              .append(beca).append(';')
              .append(honor).append(';')
              .append(horas).append("_HorasSemanales")
              .append(System.lineSeparator());
        }
        Files.writeString(salidaPath, sb.toString());
    }
}
