package com.notas.repository;

import com.notas.model.NotaCurso;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotaRepository {
    private final Path rutaArchivo;

    // Formato esperado por linea: ID;Curso;Nota;N_creditos  ej: A1;Calculo;4.5;3_creditos
    // Un mismo alumno puede tener varias lineas, una por curso cursado.
    private static final Pattern PATRON = Pattern.compile("^(.+?);(.+?);(\\d+(?:\\.\\d+)?);(\\d+)_creditos$");

    public NotaRepository(Path rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public List<NotaCurso> cargar() throws IOException {
        List<NotaCurso> notas = new ArrayList<>();
        if (!Files.exists(rutaArchivo)) return notas;

        for (String linea : Files.readAllLines(rutaArchivo)) {
            if (linea.isBlank()) continue;
            Matcher m = PATRON.matcher(linea.trim());
            if (!m.matches()) continue; // linea con formato invalido, se ignora
            String alumnoId = m.group(1);
            String curso = m.group(2);
            double nota = Double.parseDouble(m.group(3));
            int creditos = Integer.parseInt(m.group(4));
            notas.add(new NotaCurso(alumnoId, curso, nota, creditos));
        }
        return notas;
    }
}
