package com.notas.repository;

import com.notas.model.NotaCurso;
import com.notas.model.ResultadoCarga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotaRepository {
    private final Path rutaArchivo;

    // Formato por linea: AlumnoId;CursoId;Nota  ej: A1;C1;4.5
    private static final Pattern PATRON = Pattern.compile("^(.+?);(.+?);(\\d+(?:\\.\\d+)?)$");

    public NotaRepository(Path rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public ResultadoCarga<NotaCurso> cargar() throws IOException {
        List<NotaCurso> notas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        Set<String> paresVistos = new HashSet<>();
        if (!Files.exists(rutaArchivo)) return new ResultadoCarga<>(notas, errores);

        int numeroLinea = 0;
        for (String linea : Files.readAllLines(rutaArchivo)) {
            numeroLinea++;
            if (linea.isBlank()) continue;

            Matcher m = PATRON.matcher(linea.trim());
            if (!m.matches()) {
                errores.add("notas.txt linea " + numeroLinea + ": formato invalido -> \"" + linea + "\"");
                continue;
            }
            String alumnoId = m.group(1);
            String cursoId = m.group(2);
            double nota = Double.parseDouble(m.group(3));
            if (nota < 0.0 || nota > 5.0) {
                errores.add("notas.txt linea " + numeroLinea + ": nota fuera de rango (" + nota + ") en \"" + linea + "\"");
                continue;
            }
            // Un alumno no puede tener dos notas en el mismo curso: cuenta solo la primera.
            if (!paresVistos.add(alumnoId + ";" + cursoId)) {
                errores.add("notas.txt linea " + numeroLinea + ": " + alumnoId + " ya tiene nota en " + cursoId + ", se ignora");
                continue;
            }
            notas.add(new NotaCurso(alumnoId, cursoId, nota));
        }
        return new ResultadoCarga<>(notas, errores);
    }

    // Reescribe el archivo completo (usado por el CRUD). Locale.ROOT: siempre punto decimal.
    public void guardarTodos(Collection<NotaCurso> notas) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (NotaCurso n : notas) {
            sb.append(n.alumnoId()).append(';').append(n.cursoId()).append(';')
              .append(String.format(Locale.ROOT, "%.1f", n.nota())).append(System.lineSeparator());
        }
        Files.writeString(rutaArchivo, sb.toString());
    }
}
