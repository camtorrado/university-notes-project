package com.notas.repository;

import com.notas.model.PromedioAlumno;
import com.notas.service.PromedioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Escritura de promedios.csv, el archivo de salida con el ranking.
public class SalidaRepository {
    private final Path rutaArchivo;

    public SalidaRepository(Path rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    // Formato por linea: N_Puesto;ID;NombreCompleto;X.XX_Prom;SI_Beca|NO_Beca;
    // SI_Honor|NO_Honor;M_HorasSemanales  ej: 1_Puesto;A1;Carlos Perez;4.62_Prom;SI_Beca;SI_Honor;5_HorasSemanales
    public void guardar(List<PromedioAlumno> ranking, Map<String, Integer> horasPorAlumno) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (PromedioAlumno p : ranking) {
            sb.append(p.getPuesto()).append("_Puesto").append(';')
              .append(p.getId()).append(';')
              .append(p.getNombreCompleto()).append(';')
              .append(String.format(Locale.ROOT, "%.2f", p.getPromedio())).append("_Prom").append(';')
              .append(PromedioService.aplicaBeca(p.getPromedio()) ? "SI_Beca" : "NO_Beca").append(';')
              .append(PromedioService.esCuadroDeHonor(p.getPromedio()) ? "SI_Honor" : "NO_Honor").append(';')
              .append(horasPorAlumno.getOrDefault(p.getId(), 0)).append("_HorasSemanales")
              .append(System.lineSeparator());
        }
        Files.writeString(rutaArchivo, sb.toString());
    }

    // true si habia un archivo y se elimino.
    public boolean eliminar() throws IOException {
        return Files.deleteIfExists(rutaArchivo);
    }

    // null si aun no se ha exportado.
    public FileTime ultimaExportacion() throws IOException {
        return Files.exists(rutaArchivo) ? Files.getLastModifiedTime(rutaArchivo) : null;
    }

    public Path getRuta() {
        return rutaArchivo;
    }
}
