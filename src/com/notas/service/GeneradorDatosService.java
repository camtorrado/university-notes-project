package com.notas.service;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GeneradorDatosService {

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

    // Catalogos de ejemplo coherentes entre si: cada nota referencia un alumno y un curso existentes.
    public record DatosGenerados(List<Alumno> alumnos, List<Curso> cursos, List<NotaCurso> notas) {
    }

    private final Random random;

    public GeneradorDatosService() {
        this(new Random());
    }

    // Con una semilla fija los datos se repiten (util en las pruebas).
    public GeneradorDatosService(Random random) {
        this.random = random;
    }

    // Solo genera; quien llama decide donde guardarlo.
    public DatosGenerados generarEjemplos(int cantidadAlumnos, int cantidadCursos) {
        List<Curso> cursos = generarCursos(cantidadCursos);
        List<Alumno> alumnos = generarAlumnos(cantidadAlumnos);
        List<NotaCurso> notas = new ArrayList<>();
        for (Alumno alumno : alumnos) {
            notas.addAll(generarNotasAleatorias(alumno.id(), cursos));
        }
        return new DatosGenerados(alumnos, cursos, notas);
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
}
