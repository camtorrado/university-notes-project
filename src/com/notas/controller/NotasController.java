package com.notas.controller;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.PromedioAlumno;
import com.notas.model.ResultadoCarga;
import com.notas.service.GestionService;
import com.notas.service.GestionService.Base;
import com.notas.service.GestionService.ResultadoGeneracion;
import com.notas.service.OperacionInvalidaException;
import com.notas.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

// Version de consola: solo pide datos y muestra resultados, las reglas viven en GestionService.
public class NotasController {
    private static final int CANTIDAD_ALUMNOS_POR_DEFECTO = 8;
    private static final int CANTIDAD_CURSOS_POR_DEFECTO = 6;

    private final ConsoleView view = new ConsoleView();
    private final GestionService gestion = new GestionService(Path.of("data"));

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
        gestion.generarEjemplos(cantidadAlumnos, cantidadCursos);
        view.mensaje("Archivos de ejemplo generados en /data (" + cantidadAlumnos + " alumnos, " + cantidadCursos + " cursos).");
    }

    private void verBase() throws IOException {
        Base base = gestion.cargarBase();
        Map<String, Curso> cursosPorId = GestionService.mapPorId(base.cursos(), Curso::id);

        view.mostrarAlumnos(GestionService.mapPorId(base.alumnos(), Alumno::id),
            GestionService.calcularHorasPorAlumno(cursosPorId, base.notas()),
            GestionService.contarReprobadosPorAlumno(base.notas()));
        view.mostrarCursos(cursosPorId);
        view.mostrarNotas(base.notas(), cursosPorId);
        view.mostrarAdvertencias(base.advertencias());
    }

    private void verSalida() throws IOException {
        Base base = gestion.cargarBase();
        ResultadoCarga<PromedioAlumno> resultado = gestion.calcularRanking(base);
        Map<String, Curso> cursosPorId = GestionService.mapPorId(base.cursos(), Curso::id);
        view.mostrarRanking(resultado.registros(), GestionService.calcularHorasPorAlumno(cursosPorId, base.notas()),
            GestionService.contarSinNotas(base, resultado.registros()));
        view.mostrarAdvertencias(resultado.errores());
    }

    // Recalcula desde los archivos actuales, nunca queda desactualizado frente al CRUD.
    private void descargarSalida() throws IOException {
        view.mostrarAdvertencias(gestion.calcularRanking().errores());
        ejecutarRegla(() -> {
            Path ruta = gestion.exportarRanking();
            view.mensaje("Archivo exportado en " + ruta.toAbsolutePath());
        });
    }

    private void limpiarSalida() throws IOException {
        view.mensaje(gestion.limpiarSalida()
            ? "Salida eliminada."
            : "No habia ninguna salida generada.");
    }

    // ---------- CRUD Alumnos ----------

    private void gestionarAlumnos() throws IOException {
        boolean activo = true;
        while (activo) {
            Base base = gestion.cargarBase();
            Map<String, Curso> cursosPorId = GestionService.mapPorId(base.cursos(), Curso::id);
            view.mostrarAlumnos(GestionService.mapPorId(base.alumnos(), Alumno::id),
                GestionService.calcularHorasPorAlumno(cursosPorId, base.notas()),
                GestionService.contarReprobadosPorAlumno(base.notas()));
            view.mostrarSubmenuAlumnos();
            switch (view.leerOpcion()) {
                case 1 -> agregarAlumno();
                case 2 -> editarAlumno();
                case 3 -> eliminarAlumno();
                case 4 -> generarNotasAleatoriasAlumno();
                case 5 -> verFicha();
                case 6 -> activo = false;
                default -> view.mensaje("Opcion invalida.");
            }
        }
    }

    private void agregarAlumno() throws IOException {
        String nombre = view.leerTexto("Nombre completo: ");
        ejecutarRegla(() -> view.mensaje("Alumno agregado con ID " + gestion.agregarAlumno(nombre) + "."));
    }

    private void editarAlumno() throws IOException {
        String id = view.leerTexto("ID del alumno a editar: ");
        if (gestion.cargarBase().alumnos().stream().noneMatch(a -> a.id().equals(id))) {
            view.mensaje("No existe un alumno con ese ID.");
            return;
        }
        String nuevoNombre = view.leerTexto("Nuevo nombre (Enter para no cambiar): ");
        if (nuevoNombre.isBlank()) {
            view.mensaje("Alumno actualizado.");
            return;
        }
        ejecutarRegla(() -> {
            gestion.editarAlumno(id, nuevoNombre);
            view.mensaje("Alumno actualizado.");
        });
    }

    private void eliminarAlumno() throws IOException {
        String id = view.leerTexto("ID del alumno a eliminar: ");
        ejecutarRegla(() -> {
            int eliminadas = gestion.eliminarAlumno(id);
            view.mensaje("Alumno eliminado. Se eliminaron " + eliminadas + " nota(s) asociada(s).");
        });
    }

    private void generarNotasAleatoriasAlumno() throws IOException {
        String id = view.leerTexto("ID del alumno: ");
        ejecutarRegla(() -> {
            ResultadoGeneracion r = gestion.generarNotasAlumno(id);
            view.mensaje((r.reemplazo() ? "Notas recalculadas" : "Notas generadas")
                + " para " + id + " (" + r.cantidad() + " notas pseudoaleatorias).");
        });
    }

    private void verFicha() throws IOException {
        String id = view.leerTexto("ID del alumno: ");
        ejecutarRegla(() -> view.mostrarFicha(gestion.fichaAlumno(id)));
    }

    // ---------- CRUD Cursos ----------

    private void gestionarCursos() throws IOException {
        boolean activo = true;
        while (activo) {
            view.mostrarCursos(GestionService.mapPorId(gestion.cargarBase().cursos(), Curso::id));
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
        String nombre = view.leerTexto("Nombre del curso: ");
        Integer creditos = view.leerEntero("Creditos (numero entero positivo): ");
        if (creditos == null) {
            view.mensaje("Creditos invalidos, debe ser un entero positivo.");
            return;
        }
        Integer horas = view.leerEntero("Horas semanales (numero entero positivo): ");
        if (horas == null) {
            view.mensaje("Horas invalidas, debe ser un entero positivo.");
            return;
        }
        ejecutarRegla(() -> view.mensaje("Curso agregado con ID " + gestion.agregarCurso(nombre, creditos, horas) + "."));
    }

    private void editarCurso() throws IOException {
        String id = view.leerTexto("ID del curso a editar: ");
        Curso actual = gestion.cargarBase().cursos().stream()
            .filter(c -> c.id().equals(id)).findFirst().orElse(null);
        if (actual == null) {
            view.mensaje("No existe un curso con ese ID.");
            return;
        }

        String nuevoNombre = view.leerTexto("Nuevo nombre (Enter para no cambiar): ");
        String entradaCreditos = view.leerTexto("Nuevos creditos (Enter para no cambiar): ");
        String entradaHoras = view.leerTexto("Nuevas horas semanales (Enter para no cambiar): ");

        String nombreFinal = nuevoNombre.isBlank() ? actual.nombre() : nuevoNombre;
        int creditosFinal = enteroPositivoOAnterior(entradaCreditos, actual.creditos(), "Creditos");
        int horasFinal = enteroPositivoOAnterior(entradaHoras, actual.horasSemanales(), "Horas");

        ejecutarRegla(() -> {
            gestion.editarCurso(id, nombreFinal, creditosFinal, horasFinal);
            view.mensaje("Curso actualizado.");
        });
    }

    // Enter o un valor invalido mantienen el valor anterior.
    private int enteroPositivoOAnterior(String entrada, int anterior, String etiqueta) {
        if (entrada.isBlank()) return anterior;
        try {
            int valor = Integer.parseInt(entrada);
            if (valor > 0) return valor;
        } catch (NumberFormatException ignorado) {
            // Se reporta abajo.
        }
        view.mensaje(etiqueta + " invalidos, se mantiene el valor anterior.");
        return anterior;
    }

    private void eliminarCurso() throws IOException {
        String id = view.leerTexto("ID del curso a eliminar: ");
        long notasAsociadas = gestion.cargarBase().notas().stream().filter(n -> n.cursoId().equals(id)).count();
        boolean incluirNotas = notasAsociadas > 0
            && view.leerSiNo("El curso tiene " + notasAsociadas + " nota(s). Eliminar tambien sus notas?");
        ejecutarRegla(() -> {
            int eliminadas = gestion.eliminarCurso(id, incluirNotas);
            view.mensaje(eliminadas > 0
                ? "Curso eliminado junto con " + eliminadas + " nota(s)."
                : "Curso eliminado.");
        });
    }

    private void generarNotasAleatoriasCurso() throws IOException {
        String id = view.leerTexto("ID del curso: ");
        ejecutarRegla(() -> {
            ResultadoGeneracion r = gestion.generarNotasCurso(id);
            view.mensaje((r.reemplazo() ? "Notas recalculadas" : "Notas generadas")
                + " para el curso " + id + " (" + r.cantidad() + " alumno(s)).");
        });
    }

    // ---------- CRUD Notas ----------

    private void gestionarNotas() throws IOException {
        boolean activo = true;
        while (activo) {
            Base base = gestion.cargarBase();
            view.mostrarNotas(base.notas(), GestionService.mapPorId(base.cursos(), Curso::id));
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
        Base base = gestion.cargarBase();
        String alumnoId = view.leerTexto("ID del alumno: ");
        if (base.alumnos().stream().noneMatch(a -> a.id().equals(alumnoId))) {
            view.mensaje("No existe un alumno con ese ID.");
            return;
        }
        view.mostrarCursos(GestionService.mapPorId(base.cursos(), Curso::id));
        String cursoId = view.leerTexto("ID del curso: ");

        Double nota = view.leerDouble("Nota (0.0 a 5.0): ");
        if (nota == null) {
            view.mensaje("Nota invalida, debe ser un numero entre 0.0 y 5.0.");
            return;
        }
        ejecutarRegla(() -> {
            gestion.agregarNota(alumnoId, cursoId, nota);
            view.mensaje("Nota agregada.");
        });
    }

    private void editarNota() throws IOException {
        String alumnoId = view.leerTexto("ID del alumno: ");
        String cursoId = view.leerTexto("ID del curso: ");
        if (gestion.cargarBase().notas().stream()
                .noneMatch(n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId))) {
            view.mensaje("No existe una nota para ese alumno en ese curso.");
            return;
        }
        Double nuevaNota = view.leerDouble("Nueva nota (0.0 a 5.0): ");
        if (nuevaNota == null) {
            view.mensaje("Nota invalida, debe ser un numero entre 0.0 y 5.0.");
            return;
        }
        ejecutarRegla(() -> {
            gestion.editarNota(alumnoId, cursoId, nuevaNota);
            view.mensaje("Nota actualizada.");
        });
    }

    private void eliminarNota() throws IOException {
        String alumnoId = view.leerTexto("ID del alumno: ");
        String cursoId = view.leerTexto("ID del curso: ");
        ejecutarRegla(() -> {
            gestion.eliminarNota(alumnoId, cursoId);
            view.mensaje("Nota eliminada.");
        });
    }

    // ---------- Utilidades ----------

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws IOException;
    }

    // Si se incumple una regla de negocio, muestra el motivo en vez de cortar el programa.
    private void ejecutarRegla(Accion accion) throws IOException {
        try {
            accion.ejecutar();
        } catch (OperacionInvalidaException e) {
            view.mensaje(e.getMessage());
        }
    }
}
