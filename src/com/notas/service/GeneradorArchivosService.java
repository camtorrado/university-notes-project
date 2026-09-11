package com.notas.service;

import com.notas.model.PromedioAlumno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

public class GeneradorArchivosService {

    private static final List<String> NOMBRES = List.of(
        "Carlos", "Maria", "Andres", "Laura", "Juan", "Camila", "Diego", "Valentina",
        "Santiago", "Isabella", "Felipe", "Daniela", "Alejandro", "Gabriela", "Sebastian"
    );
    private static final List<String> APELLIDOS = List.of(
        "Perez", "Gomez", "Rodriguez", "Martinez", "Lopez", "Garcia", "Hernandez",
        "Torres", "Ramirez", "Vargas", "Castro", "Ortiz", "Rojas", "Molina"
    );
    private static final List<String> CURSOS = List.of(
        "Calculo", "Algebra Lineal", "Programacion", "Estructuras de Datos", "Fisica",
        "Bases de Datos", "Redes", "Ingles", "Estadistica", "Arquitectura de Software"
    );
    private static final int[] CREDITOS_POSIBLES = {1, 2, 3, 4};

    private final Random random = new Random();

    // Genera alumnos.csv y notas.txt de forma pseudoaleatoria pero coherente entre si:
    // cada alumno generado tiene entre 3 y 6 notas, y los IDs coinciden en ambos archivos.
    public void generarEjemplos(Path alumnosPath, Path notasPath, int cantidadAlumnos) throws IOException {
        StringBuilder alumnosSb = new StringBuilder();
        StringBuilder notasSb = new StringBuilder();

        for (int i = 1; i <= cantidadAlumnos; i++) {
            String id = "A" + i;
            alumnosSb.append(id).append(';').append(nombreAleatorio()).append(System.lineSeparator());

            int cantidadCursos = 3 + random.nextInt(4); // entre 3 y 6 cursos por alumno
            for (int c = 0; c < cantidadCursos; c++) {
                String curso = CURSOS.get(random.nextInt(CURSOS.size()));
                int creditos = CREDITOS_POSIBLES[random.nextInt(CREDITOS_POSIBLES.length)];
                notasSb.append(id).append(';').append(curso).append(';')
                       .append(String.format("%.1f", notaAleatoria())).append(';')
                       .append(creditos).append("_creditos")
                       .append(System.lineSeparator());
            }
        }

        Files.writeString(alumnosPath, alumnosSb.toString());
        Files.writeString(notasPath, notasSb.toString());
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

    // Formato de salida por linea: ID;NombreCompleto;X.X_Prom  ej: A1;Carlos Perez;4.2_Prom
    public void exportar(Path salidaPath, List<PromedioAlumno> ranking) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (PromedioAlumno p : ranking) {
            sb.append(p.getId()).append(';')
              .append(p.getNombreCompleto()).append(';')
              .append(String.format("%.1f", p.getPromedio())).append("_Prom")
              .append(System.lineSeparator());
        }
        Files.writeString(salidaPath, sb.toString());
    }
}
