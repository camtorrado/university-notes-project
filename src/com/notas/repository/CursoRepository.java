package com.notas.repository;

import com.notas.model.Curso;
import com.notas.model.ResultadoCarga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CursoRepository {
    private final Path rutaArchivo;

    // Formato esperado por linea: ID;Nombre;N_creditos;M_horas  ej: C1;Calculo;3_creditos;4_horas
    private static final Pattern PATRON = Pattern.compile("^(.+?);(.+?);(\\d+)_creditos;(\\d+)_horas$");

    public CursoRepository(Path rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public ResultadoCarga<Curso> cargar() throws IOException {
        List<Curso> cursos = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();
        if (!Files.exists(rutaArchivo)) return new ResultadoCarga<>(cursos, errores);

        int numeroLinea = 0;
        for (String linea : Files.readAllLines(rutaArchivo)) {
            numeroLinea++;
            if (linea.isBlank()) continue;

            Matcher m = PATRON.matcher(linea.trim());
            if (!m.matches()) {
                errores.add("cursos.csv linea " + numeroLinea + ": formato invalido -> \"" + linea + "\"");
                continue;
            }
            String id = m.group(1);
            String nombre = m.group(2);
            int creditos = Integer.parseInt(m.group(3));
            int horas = Integer.parseInt(m.group(4));
            if (creditos <= 0 || horas <= 0) {
                errores.add("cursos.csv linea " + numeroLinea + ": creditos u horas invalidos en \"" + linea + "\"");
                continue;
            }
            if (!idsVistos.add(id)) {
                errores.add("cursos.csv linea " + numeroLinea + ": ID duplicado \"" + id + "\", se ignora");
                continue;
            }
            cursos.add(new Curso(id, nombre, creditos, horas));
        }
        return new ResultadoCarga<>(cursos, errores);
    }

    // Reescribe el archivo completo (usado por el CRUD).
    public void guardarTodos(Collection<Curso> cursos) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Curso c : cursos) {
            sb.append(c.id()).append(';').append(c.nombre()).append(';')
              .append(c.creditos()).append("_creditos").append(';')
              .append(c.horasSemanales()).append("_horas").append(System.lineSeparator());
        }
        Files.writeString(rutaArchivo, sb.toString());
    }
}
