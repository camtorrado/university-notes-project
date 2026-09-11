package com.notas.repository;

import com.notas.model.Alumno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class AlumnoRepository {
    private final Path rutaArchivo;

    public AlumnoRepository(Path rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    // Formato esperado por linea: ID;NombreCompleto  ej: A1;Carlos Perez
    public Map<String, Alumno> cargar() throws IOException {
        Map<String, Alumno> alumnos = new LinkedHashMap<>();
        if (!Files.exists(rutaArchivo)) return alumnos;

        for (String linea : Files.readAllLines(rutaArchivo)) {
            if (linea.isBlank()) continue;
            String[] campos = linea.split(";", 2);
            String id = campos[0].trim();
            String nombre = campos[1].trim();
            alumnos.put(id, new Alumno(id, nombre));
        }
        return alumnos;
    }
}
