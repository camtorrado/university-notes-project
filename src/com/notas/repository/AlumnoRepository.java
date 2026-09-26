package com.notas.repository;

import com.notas.model.Alumno;
import com.notas.model.ResultadoCarga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlumnoRepository {
    private final Path rutaArchivo;

    public AlumnoRepository(Path rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    // Formato esperado por linea: ID;NombreCompleto  ej: A1;Carlos Perez
    public ResultadoCarga<Alumno> cargar() throws IOException {
        List<Alumno> alumnos = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();
        if (!Files.exists(rutaArchivo)) return new ResultadoCarga<>(alumnos, errores);

        int numeroLinea = 0;
        for (String linea : Files.readAllLines(rutaArchivo)) {
            numeroLinea++;
            if (linea.isBlank()) continue;

            String[] campos = linea.split(";", 2);
            if (campos.length < 2 || campos[0].isBlank() || campos[1].isBlank()) {
                errores.add("alumnos.csv linea " + numeroLinea + ": formato invalido -> \"" + linea + "\"");
                continue;
            }
            String id = campos[0].trim();
            String nombre = campos[1].trim();
            if (!idsVistos.add(id)) {
                errores.add("alumnos.csv linea " + numeroLinea + ": ID duplicado \"" + id + "\", se ignora");
                continue;
            }
            alumnos.add(new Alumno(id, nombre));
        }
        return new ResultadoCarga<>(alumnos, errores);
    }

    // Reescribe el archivo completo (usado por el CRUD).
    public void guardarTodos(Collection<Alumno> alumnos) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Alumno a : alumnos) {
            sb.append(a.id()).append(';').append(a.nombreCompleto()).append(System.lineSeparator());
        }
        Files.writeString(rutaArchivo, sb.toString());
    }
}
