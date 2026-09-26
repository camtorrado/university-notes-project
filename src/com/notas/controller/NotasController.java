package com.notas.controller;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.model.ResultadoCarga;
import com.notas.repository.AlumnoRepository;
import com.notas.repository.CursoRepository;
import com.notas.repository.NotaRepository;
import com.notas.service.GeneradorArchivosService;
import com.notas.service.PromedioService;
import com.notas.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotasController {
    private static final Path DIR_DATOS = Path.of("data");
    private static final Path ALUMNOS_PATH = DIR_DATOS.resolve("alumnos.csv");
    private static final Path CURSOS_PATH = DIR_DATOS.resolve("cursos.csv");
    private static final Path NOTAS_PATH = DIR_DATOS.resolve("notas.txt");
    private static final Path SALIDA_PATH = DIR_DATOS.resolve("promedios.csv");

    private static final int CANTIDAD_ALUMNOS_POR_DEFECTO = 8;
    private static final int CANTIDAD_CURSOS_POR_DEFECTO = 6;

    private final ConsoleView view = new ConsoleView();
    private final AlumnoRepository alumnoRepo = new AlumnoRepository(ALUMNOS_PATH);
    private final CursoRepository cursoRepo = new CursoRepository(CURSOS_PATH);
    private final NotaRepository notaRepo = new NotaRepository(NOTAS_PATH);
    private final PromedioService promedioService = new PromedioService();
    private final GeneradorArchivosService generador = new GeneradorArchivosService();

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
                    case 5 -> limpiarSalida();
                    case 6 -> gestionarAlumnos();
                    case 7 -> gestionarCursos();
                    case 8 -> gestionarNotas();
                    case 9 -> view.mostrarAcercaDe();
                    case 10 -> activo = false;
                    default -> view.mensaje("Opcion invalida.");
                }
            } catch (IOException e) {
                view.mensaje("Error de archivo: " + e.getMessage());
            }
        }
        view.mensaje("Hasta luego.");
    }

    // ---------- Opciones principales ----------

    private void generarArchivos() throws IOException {
        int cantidadAlumnos = view.leerCantidad("alumnos", CANTIDAD_ALUMNOS_POR_DEFECTO);
        int cantidadCursos = view.leerCantidad("cursos", CANTIDAD_CURSOS_POR_DEFECTO);
        Files.createDirectories(DIR_DATOS);
        generador.generarEjemplos(ALUMNOS_PATH, CURSOS_PATH, NOTAS_PATH, cantidadAlumnos, cantidadCursos);
        view.mensaje("Archivos de ejemplo generados en /data (" + cantidadAlumnos + " alumnos, " + cantidadCursos + " cursos).");
    }

    private void verBase() throws IOException {
        ResultadoCarga<Alumno> alumnos = alumnoRepo.cargar();
        ResultadoCarga<Curso> cursos = cursoRepo.cargar();
        ResultadoCarga<NotaCurso> notas = notaRepo.cargar();
        Map<String, Curso> cursosPorId = mapPorId(cursos.registros(), Curso::id);

        view.mostrarAlumnos(mapPorId(alumnos.registros(), Alumno::id),
            calcularHorasPorAlumno(cursosPorId, notas.registros()));
        view.mostrarCursos(cursosPorId);
        view.mostrarNotas(notas.registros(), cursosPorId);

        List<String> advertencias = new ArrayList<>();
        advertencias.addAll(alumnos.errores());
        advertencias.addAll(cursos.errores());
        advertencias.addAll(notas.errores());
        view.mostrarAdvertencias(advertencias);
    }

    private void verSalida() throws IOException {
        ResultadoCarga<PromedioAlumno> resultado = calcularRankingActual();
        Map<String, Curso> cursosPorId = mapPorId(cursoRepo.cargar().registros(), Curso::id);
        Map<String, Integer> horasPorAlumno = calcularHorasPorAlumno(cursosPorId, notaRepo.cargar().registros());
        view.mostrarRanking(resultado.registros(), horasPorAlumno);
        view.mostrarAdvertencias(resultado.errores());
    }

    // Recalcula desde los archivos actuales, nunca queda desactualizado frente al CRUD.
    private void descargarSalida() throws IOException {
        ResultadoCarga<PromedioAlumno> resultado = calcularRankingActual();
        view.mostrarAdvertencias(resultado.errores());

        if (resultado.registros().isEmpty()) {
            view.mensaje("No hay datos para exportar: revise que haya alumnos, cursos y notas cargados.");
            return;
        }
        Map<String, Curso> cursosPorId = mapPorId(cursoRepo.cargar().registros(), Curso::id);
        Map<String, Integer> horasPorAlumno = calcularHorasPorAlumno(cursosPorId, notaRepo.cargar().registros());
        generador.exportar(SALIDA_PATH, resultado.registros(), horasPorAlumno);
        view.mensaje("Archivo exportado en " + SALIDA_PATH.toAbsolutePath());
    }

    private void limpiarSalida() throws IOException {
        boolean existia = Files.deleteIfExists(SALIDA_PATH);
        view.mensaje(existia
            ? "Salida eliminada."
            : "No habia ninguna salida generada.");
    }

    // Usado por [3] y [4] para que ambos vean siempre el mismo estado de los datos.
    private ResultadoCarga<PromedioAlumno> calcularRankingActual() throws IOException {
        ResultadoCarga<Alumno> alumnos = alumnoRepo.cargar();
        ResultadoCarga<Curso> cursos = cursoRepo.cargar();
        ResultadoCarga<NotaCurso> notas = notaRepo.cargar();

        Map<String, Alumno> alumnosPorId = mapPorId(alumnos.registros(), Alumno::id);
        Map<String, Curso> cursosPorId = mapPorId(cursos.registros(), Curso::id);
        ResultadoCarga<PromedioAlumno> calculo = promedioService.calcularRanking(alumnosPorId, cursosPorId, notas.registros());

        List<String> advertencias = new ArrayList<>();
        advertencias.addAll(alumnos.errores());
        advertencias.addAll(cursos.errores());
        advertencias.addAll(notas.errores());
        advertencias.addAll(calculo.errores());
        return new ResultadoCarga<>(calculo.registros(), advertencias);
    }

    private <T> Map<String, T> mapPorId(List<T> lista, java.util.function.Function<T, String> idExtractor) {
        return lista.stream().collect(Collectors.toMap(idExtractor, x -> x, (a, b) -> a, LinkedHashMap::new));
    }

    // Suma las horas semanales de cada curso en el que el alumno tiene nota (esta matriculado).
    private Map<String, Integer> calcularHorasPorAlumno(Map<String, Curso> cursosPorId, List<NotaCurso> notas) {
        Map<String, Integer> horasPorAlumno = new LinkedHashMap<>();
        for (NotaCurso nota : notas) {
            Curso curso = cursosPorId.get(nota.cursoId());
            if (curso == null) continue;
            horasPorAlumno.merge(nota.alumnoId(), curso.horasSemanales(), Integer::sum);
        }
        return horasPorAlumno;
    }

    // ---------- CRUD Alumnos ----------

    private void gestionarAlumnos() throws IOException {
        boolean activo = true;
        while (activo) {
            Map<String, Curso> cursosPorId = mapPorId(cursoRepo.cargar().registros(), Curso::id);
            List<NotaCurso> notas = notaRepo.cargar().registros();
            view.mostrarAlumnos(mapPorId(alumnoRepo.cargar().registros(), Alumno::id),
                calcularHorasPorAlumno(cursosPorId, notas));
            view.mostrarSubmenuAlumnos();
            switch (view.leerOpcion()) {
                case 1 -> agregarAlumno();
                case 2 -> editarAlumno();
                case 3 -> eliminarAlumno();
                case 4 -> generarNotasAleatoriasAlumno();
                case 5 -> activo = false;
                default -> view.mensaje("Opcion invalida.");
            }
        }
    }

    private void agregarAlumno() throws IOException {
        List<Alumno> alumnos = new ArrayList<>(alumnoRepo.cargar().registros());

        String nombre = view.leerTexto("Nombre completo: ");
        if (nombre.isBlank()) {
            view.mensaje("El nombre no puede estar vacio.");
            return;
        }
        if (nombreDuplicado(alumnos.stream().map(Alumno::nombreCompleto).toList(), nombre)) {
            view.mensaje("Ya existe un alumno con ese nombre.");
            return;
        }

        String id = generarSiguienteId(alumnos.stream().map(Alumno::id).toList(), "A");
        alumnos.add(new Alumno(id, nombre));
        alumnoRepo.guardarTodos(alumnos);
        view.mensaje("Alumno agregado con ID " + id + ".");
    }

    private void editarAlumno() throws IOException {
        List<Alumno> alumnos = new ArrayList<>(alumnoRepo.cargar().registros());
        String id = view.leerTexto("ID del alumno a editar: ");
        int indice = indiceDe(alumnos, a -> a.id().equals(id));
        if (indice < 0) {
            view.mensaje("No existe un alumno con ese ID.");
            return;
        }
        String nuevoNombre = view.leerTexto("Nuevo nombre (Enter para no cambiar): ");
        if (!nuevoNombre.isBlank()) {
            List<String> otrosNombres = alumnos.stream()
                .filter(a -> !a.id().equals(id)).map(Alumno::nombreCompleto).toList();
            if (nombreDuplicado(otrosNombres, nuevoNombre)) {
                view.mensaje("Ya existe un alumno con ese nombre.");
                return;
            }
            alumnos.set(indice, new Alumno(id, nuevoNombre));
            alumnoRepo.guardarTodos(alumnos);
        }
        view.mensaje("Alumno actualizado.");
    }

    private void eliminarAlumno() throws IOException {
        List<Alumno> alumnos = new ArrayList<>(alumnoRepo.cargar().registros());
        String id = view.leerTexto("ID del alumno a eliminar: ");
        boolean existia = alumnos.removeIf(a -> a.id().equals(id));
        if (!existia) {
            view.mensaje("No existe un alumno con ese ID.");
            return;
        }
        alumnoRepo.guardarTodos(alumnos);

        // Se eliminan tambien las notas del alumno para no dejar referencias huerfanas en notas.txt.
        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        int eliminadas = (int) notas.stream().filter(n -> n.alumnoId().equals(id)).count();
        notas.removeIf(n -> n.alumnoId().equals(id));
        notaRepo.guardarTodos(notas);

        view.mensaje("Alumno eliminado. Se eliminaron " + eliminadas + " nota(s) asociada(s).");
    }

    // Feature aparte del CRUD: genera notas pseudoaleatorias para un alumno (recalcula si ya tenia).
    private void generarNotasAleatoriasAlumno() throws IOException {
        List<Alumno> alumnos = alumnoRepo.cargar().registros();
        String id = view.leerTexto("ID del alumno: ");
        if (alumnos.stream().noneMatch(a -> a.id().equals(id))) {
            view.mensaje("No existe un alumno con ese ID.");
            return;
        }

        List<Curso> cursos = cursoRepo.cargar().registros();
        if (cursos.isEmpty()) {
            view.mensaje("No hay cursos registrados. Cree al menos un curso primero.");
            return;
        }

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        boolean teniaNotas = notas.removeIf(n -> n.alumnoId().equals(id));

        List<NotaCurso> notasNuevas = generador.generarNotasAleatorias(id, cursos);
        notas.addAll(notasNuevas);
        notaRepo.guardarTodos(notas);

        view.mensaje((teniaNotas ? "Notas recalculadas" : "Notas generadas")
            + " para " + id + " (" + notasNuevas.size() + " notas pseudoaleatorias).");
    }

    // ---------- CRUD Cursos ----------

    private void gestionarCursos() throws IOException {
        boolean activo = true;
        while (activo) {
            view.mostrarCursos(mapPorId(cursoRepo.cargar().registros(), Curso::id));
            view.mostrarSubmenuCursos();
            switch (view.leerOpcion()) {
                case 1 -> agregarCurso();
                case 2 -> editarCurso();
                case 3 -> eliminarCurso();
                case 4 -> generarNotasAleatoriasCurso();
                case 5 -> activo = false;
                default -> view.mensaje("Opcion invalida.");
            }
        }
    }

    private void agregarCurso() throws IOException {
        List<Curso> cursos = new ArrayList<>(cursoRepo.cargar().registros());

        String nombre = view.leerTexto("Nombre del curso: ");
        if (nombre.isBlank()) {
            view.mensaje("El nombre no puede estar vacio.");
            return;
        }
        if (nombreDuplicado(cursos.stream().map(Curso::nombre).toList(), nombre)) {
            view.mensaje("Ya existe un curso con ese nombre.");
            return;
        }
        Integer creditos = view.leerEntero("Creditos (numero entero positivo): ");
        if (creditos == null || creditos <= 0) {
            view.mensaje("Creditos invalidos, debe ser un entero positivo.");
            return;
        }
        Integer horas = view.leerEntero("Horas semanales (numero entero positivo): ");
        if (horas == null || horas <= 0) {
            view.mensaje("Horas invalidas, debe ser un entero positivo.");
            return;
        }

        String id = generarSiguienteId(cursos.stream().map(Curso::id).toList(), "C");
        cursos.add(new Curso(id, nombre, creditos, horas));
        cursoRepo.guardarTodos(cursos);
        view.mensaje("Curso agregado con ID " + id + ".");
    }

    private void editarCurso() throws IOException {
        List<Curso> cursos = new ArrayList<>(cursoRepo.cargar().registros());
        String id = view.leerTexto("ID del curso a editar: ");
        int indice = indiceDe(cursos, c -> c.id().equals(id));
        if (indice < 0) {
            view.mensaje("No existe un curso con ese ID.");
            return;
        }
        Curso actual = cursos.get(indice);

        String nuevoNombre = view.leerTexto("Nuevo nombre (Enter para no cambiar): ");
        String entradaCreditos = view.leerTexto("Nuevos creditos (Enter para no cambiar): ");
        String entradaHoras = view.leerTexto("Nuevas horas semanales (Enter para no cambiar): ");

        if (!nuevoNombre.isBlank()) {
            List<String> otrosNombres = cursos.stream()
                .filter(c -> !c.id().equals(id)).map(Curso::nombre).toList();
            if (nombreDuplicado(otrosNombres, nuevoNombre)) {
                view.mensaje("Ya existe un curso con ese nombre.");
                return;
            }
        }

        String nombreFinal = nuevoNombre.isBlank() ? actual.nombre() : nuevoNombre;
        int creditosFinal = actual.creditos();
        if (!entradaCreditos.isBlank()) {
            try {
                int valor = Integer.parseInt(entradaCreditos);
                if (valor <= 0) {
                    view.mensaje("Creditos invalidos, se mantiene el valor anterior.");
                } else {
                    creditosFinal = valor;
                }
            } catch (NumberFormatException e) {
                view.mensaje("Creditos invalidos, se mantiene el valor anterior.");
            }
        }

        int horasFinal = actual.horasSemanales();
        if (!entradaHoras.isBlank()) {
            try {
                int valor = Integer.parseInt(entradaHoras);
                if (valor <= 0) {
                    view.mensaje("Horas invalidas, se mantiene el valor anterior.");
                } else {
                    horasFinal = valor;
                }
            } catch (NumberFormatException e) {
                view.mensaje("Horas invalidas, se mantiene el valor anterior.");
            }
        }

        cursos.set(indice, new Curso(id, nombreFinal, creditosFinal, horasFinal));
        cursoRepo.guardarTodos(cursos);
        view.mensaje("Curso actualizado.");
    }

    private void eliminarCurso() throws IOException {
        List<Curso> cursos = new ArrayList<>(cursoRepo.cargar().registros());
        String id = view.leerTexto("ID del curso a eliminar: ");
        if (cursos.stream().noneMatch(c -> c.id().equals(id))) {
            view.mensaje("No existe un curso con ese ID.");
            return;
        }

        List<NotaCurso> notas = notaRepo.cargar().registros();
        long notasAsociadas = notas.stream().filter(n -> n.cursoId().equals(id)).count();
        if (notasAsociadas > 0) {
            view.mensaje("No se puede eliminar: hay " + notasAsociadas
                + " nota(s) asociada(s) a este curso. Elimine o reasigne esas notas primero.");
            return;
        }

        cursos.removeIf(c -> c.id().equals(id));
        cursoRepo.guardarTodos(cursos);
        view.mensaje("Curso eliminado.");
    }

    // Simetrica a la de alumnos: genera una nota pseudoaleatoria en el curso para cada alumno.
    private void generarNotasAleatoriasCurso() throws IOException {
        List<Curso> cursos = cursoRepo.cargar().registros();
        String id = view.leerTexto("ID del curso: ");
        if (cursos.stream().noneMatch(c -> c.id().equals(id))) {
            view.mensaje("No existe un curso con ese ID.");
            return;
        }

        List<Alumno> alumnos = alumnoRepo.cargar().registros();
        if (alumnos.isEmpty()) {
            view.mensaje("No hay alumnos registrados. Cree al menos un alumno primero.");
            return;
        }

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        boolean teniaNotas = notas.removeIf(n -> n.cursoId().equals(id));

        List<NotaCurso> notasNuevas = generador.generarNotasAleatoriasParaCurso(id, alumnos);
        notas.addAll(notasNuevas);
        notaRepo.guardarTodos(notas);

        view.mensaje((teniaNotas ? "Notas recalculadas" : "Notas generadas")
            + " para el curso " + id + " (" + notasNuevas.size() + " alumno(s)).");
    }

    // ---------- CRUD Notas ----------

    private void gestionarNotas() throws IOException {
        boolean activo = true;
        while (activo) {
            Map<String, Curso> cursosPorId = mapPorId(cursoRepo.cargar().registros(), Curso::id);
            view.mostrarNotas(notaRepo.cargar().registros(), cursosPorId);
            view.mostrarSubmenuNotas();
            switch (view.leerOpcion()) {
                case 1 -> agregarNota();
                case 2 -> editarNota();
                case 3 -> eliminarNota();
                case 4 -> activo = false;
                default -> view.mensaje("Opcion invalida.");
            }
        }
    }

    private void agregarNota() throws IOException {
        Map<String, Alumno> alumnos = mapPorId(alumnoRepo.cargar().registros(), Alumno::id);
        Map<String, Curso> cursos = mapPorId(cursoRepo.cargar().registros(), Curso::id);

        String alumnoId = view.leerTexto("ID del alumno: ");
        if (!alumnos.containsKey(alumnoId)) {
            view.mensaje("No existe un alumno con ese ID.");
            return;
        }

        view.mostrarCursos(cursos);
        String cursoId = view.leerTexto("ID del curso: ");
        if (!cursos.containsKey(cursoId)) {
            view.mensaje("No existe un curso con ese ID.");
            return;
        }

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        if (notas.stream().anyMatch(n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId))) {
            view.mensaje("Ya existe una nota para ese alumno en ese curso. Use Editar en su lugar.");
            return;
        }

        Double nota = view.leerDouble("Nota (0.0 a 5.0): ");
        if (nota == null || nota < 0.0 || nota > 5.0) {
            view.mensaje("Nota invalida, debe ser un numero entre 0.0 y 5.0.");
            return;
        }

        notas.add(new NotaCurso(alumnoId, cursoId, nota));
        notaRepo.guardarTodos(notas);
        view.mensaje("Nota agregada.");
    }

    private void editarNota() throws IOException {
        String alumnoId = view.leerTexto("ID del alumno: ");
        String cursoId = view.leerTexto("ID del curso: ");

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        int indice = indiceDe(notas, n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId));
        if (indice < 0) {
            view.mensaje("No existe una nota para ese alumno en ese curso.");
            return;
        }

        Double nuevaNota = view.leerDouble("Nueva nota (0.0 a 5.0): ");
        if (nuevaNota == null || nuevaNota < 0.0 || nuevaNota > 5.0) {
            view.mensaje("Nota invalida, debe ser un numero entre 0.0 y 5.0.");
            return;
        }

        notas.set(indice, new NotaCurso(alumnoId, cursoId, nuevaNota));
        notaRepo.guardarTodos(notas);
        view.mensaje("Nota actualizada.");
    }

    private void eliminarNota() throws IOException {
        String alumnoId = view.leerTexto("ID del alumno: ");
        String cursoId = view.leerTexto("ID del curso: ");

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        boolean existia = notas.removeIf(n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId));
        if (!existia) {
            view.mensaje("No existe una nota para ese alumno en ese curso.");
            return;
        }

        notaRepo.guardarTodos(notas);
        view.mensaje("Nota eliminada.");
    }

    // Compara ignorando mayusculas/minusculas, para no permitir el mismo nombre en dos variantes.
    private boolean nombreDuplicado(List<String> nombresExistentes, String nombreNuevo) {
        return nombresExistentes.stream().anyMatch(n -> n.equalsIgnoreCase(nombreNuevo));
    }

    private <T> int indiceDe(List<T> lista, java.util.function.Predicate<T> condicion) {
        for (int i = 0; i < lista.size(); i++) {
            if (condicion.test(lista.get(i))) return i;
        }
        return -1;
    }

    // Numero mas alto entre los IDs existentes con ese prefijo, mas uno.
    private String generarSiguienteId(List<String> idsExistentes, String prefijo) {
        int maximo = 0;
        for (String id : idsExistentes) {
            if (!id.startsWith(prefijo)) continue;
            try {
                maximo = Math.max(maximo, Integer.parseInt(id.substring(prefijo.length())));
            } catch (NumberFormatException ignorado) {
                // Formato distinto al esperado, se ignora.
            }
        }
        return prefijo + (maximo + 1);
    }
}
