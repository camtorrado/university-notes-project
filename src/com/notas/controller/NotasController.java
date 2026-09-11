package com.notas.controller;

import com.notas.model.Alumno;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.repository.AlumnoRepository;
import com.notas.repository.NotaRepository;
import com.notas.service.GeneradorArchivosService;
import com.notas.service.PromedioService;
import com.notas.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class NotasController {
    private static final Path DIR_DATOS = Path.of("data");
    private static final Path ALUMNOS_PATH = DIR_DATOS.resolve("alumnos.csv");
    private static final Path NOTAS_PATH = DIR_DATOS.resolve("notas.txt");
    private static final Path SALIDA_PATH = DIR_DATOS.resolve("promedios.csv");

    private static final int CANTIDAD_ALUMNOS_POR_DEFECTO = 8;

    private final ConsoleView view = new ConsoleView();
    private final AlumnoRepository alumnoRepo = new AlumnoRepository(ALUMNOS_PATH);
    private final NotaRepository notaRepo = new NotaRepository(NOTAS_PATH);
    private final PromedioService promedioService = new PromedioService();
    private final GeneradorArchivosService generador = new GeneradorArchivosService();

    private List<PromedioAlumno> ultimoRanking = List.of();

    public void ejecutar() {
        boolean activo = true;
        while (activo) {
            view.mostrarMenu();
            int opcion = view.leerOpcion();
            try {
                switch (opcion) {
                    case 1 -> generarArchivos();
                    case 2 -> verBase();
                    case 3 -> verSalida();
                    case 4 -> descargarSalida();
                    case 5 -> activo = false;
                    default -> view.mensaje("Opcion invalida.");
                }
            } catch (IOException e) {
                view.mensaje("Error de archivo: " + e.getMessage());
            }
        }
        view.mensaje("Hasta luego.");
    }

    private void generarArchivos() throws IOException {
        int cantidadAlumnos = view.leerCantidadAlumnos(CANTIDAD_ALUMNOS_POR_DEFECTO);
        Files.createDirectories(DIR_DATOS);
        generador.generarEjemplos(ALUMNOS_PATH, NOTAS_PATH, cantidadAlumnos);
        view.mensaje("Archivos de ejemplo generados en /data (" + cantidadAlumnos + " alumnos).");
    }

    private void verBase() throws IOException {
        view.mostrarAlumnos(alumnoRepo.cargar());
        view.mostrarNotas(notaRepo.cargar());
    }

    private void verSalida() throws IOException {
        Map<String, Alumno> alumnos = alumnoRepo.cargar();
        List<NotaCurso> notas = notaRepo.cargar();
        ultimoRanking = promedioService.calcularRanking(alumnos, notas);
        view.mostrarRanking(ultimoRanking);
    }

    private void descargarSalida() throws IOException {
        if (ultimoRanking.isEmpty()) {
            view.mensaje("Primero genere la salida con la opcion 3.");
            return;
        }
        generador.exportar(SALIDA_PATH, ultimoRanking);
        view.mensaje("Archivo exportado en " + SALIDA_PATH.toAbsolutePath());
    }
}
