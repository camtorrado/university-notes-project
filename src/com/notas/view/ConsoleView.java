package com.notas.view;

import com.notas.Proyecto;
import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.service.GestionService.Ficha;
import com.notas.service.GestionService.NotaFicha;
import com.notas.service.PromedioService;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class ConsoleView {
    private final Scanner scanner = new Scanner(System.in);

    public void mostrarMenu() {
        System.out.println("""
            
            ================================================
            Bienvenid@ a nuestro programa de notas
                            universitarias
            ================================================
            
            C:\\> java main
            [1] Generar Archivos
            [2] Importar/Ver Base
            [3] Importar/Ver Salida
            [4] Descargar Salida
            [5] Limpiar Salida
            [6] Gestionar Alumnos
            [7] Gestionar Cursos
            [8] Gestionar Notas
            [9] Acerca de
            [10] Salir
            """);
        System.out.print("Seleccione una opcion: ");
    }

    public void mostrarSubmenuAlumnos() {
        System.out.println("""

            --- Gestionar Alumnos ---
            [1] Agregar alumno
            [2] Editar alumno
            [3] Eliminar alumno
            [4] Generar notas aleatorias
            [5] Ver ficha
            [6] Volver
            """);
        System.out.print("Seleccione una opcion: ");
    }

    public void mostrarSubmenuCursos() {
        System.out.println("""

            --- Gestionar Cursos ---
            [1] Agregar curso
            [2] Editar curso
            [3] Eliminar curso
            [4] Generar notas del curso
            [5] Volver
            """);
        System.out.print("Seleccione una opcion: ");
    }

    public void mostrarSubmenuNotas() {
        System.out.println("""

            --- Gestionar Notas ---
            [1] Agregar nota
            [2] Editar nota
            [3] Eliminar nota
            [4] Volver
            """);
        System.out.print("Seleccione una opcion: ");
    }

    public int leerOpcion() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Si la entrada no es un entero positivo, usa el valor por defecto.
    public int leerCantidad(String etiqueta, int porDefecto) {
        System.out.print("Cantidad de " + etiqueta + " a generar (Enter para " + porDefecto + "): ");
        String entrada = scanner.nextLine().trim();
        if (entrada.isBlank()) return porDefecto;
        try {
            int cantidad = Integer.parseInt(entrada);
            return cantidad > 0 ? cantidad : porDefecto;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    // El llamador valida si puede ser vacio.
    public String leerTexto(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // Null si no es un numero valido, el llamador decide como reaccionar.
    public Double leerDouble(String prompt) {
        System.out.print(prompt);
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // true solo si la respuesta empieza por s/S.
    public boolean leerSiNo(String prompt) {
        System.out.print(prompt + " (s/n): ");
        return scanner.nextLine().trim().toLowerCase(Locale.ROOT).startsWith("s");
    }

    public Integer leerEntero(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // horasPorAlumno trae la carga horaria semanal ya sumada (segun los cursos que curse).
    public void mostrarAlumnos(Map<String, Alumno> alumnos, Map<String, Integer> horasPorAlumno,
                               Map<String, Integer> reprobadosPorAlumno) {
        System.out.println("\n--- ALUMNOS ---");
        alumnos.values().forEach(a -> {
            int horas = horasPorAlumno.getOrDefault(a.id(), 0);
            int reprobados = reprobadosPorAlumno.getOrDefault(a.id(), 0);
            System.out.printf("%-5s %-25s %3dh semanales   %s%n", a.id(), a.nombreCompleto(), horas,
                reprobados == 0 ? "" : reprobados + " reprobada(s)");
        });
    }

    public void mostrarCursos(Map<String, Curso> cursos) {
        System.out.println("\n--- CURSOS ---");
        cursos.values().forEach(c ->
            System.out.printf("%-5s %-25s %d creditos  %dh semanales%n",
                c.id(), c.nombre(), c.creditos(), c.horasSemanales()));
    }

    public void mostrarNotas(List<NotaCurso> notas, Map<String, Curso> cursos) {
        System.out.println("\n--- NOTAS ---");
        for (NotaCurso n : notas) {
            Curso curso = cursos.get(n.cursoId());
            String nombreCurso = curso != null ? curso.nombre() : "(curso desconocido)";
            System.out.printf(Locale.ROOT, "%-5s %-5s %-25s %.1f %s%n", n.alumnoId(), n.cursoId(), nombreCurso, n.nota(),
                PromedioService.estaAprobada(n.nota()) ? "" : "(reprobada)");
        }
    }

    // El puesto ya viene calculado (los empates lo comparten).
    public void mostrarRanking(List<PromedioAlumno> ranking, Map<String, Integer> horasPorAlumno, long alumnosSinNotas) {
        System.out.println("\n--- CUADRO DE HONOR / RANKING (ordenado por promedio) ---");
        System.out.printf("%-6s %-5s %-25s %-10s %-10s %-10s %s%n", "Puesto", "ID", "Nombre", "Promedio", "Beca", "Horas", "");
        for (PromedioAlumno p : ranking) {
            String beca = PromedioService.aplicaBeca(p.getPromedio()) ? "SI" : "NO";
            String honor = PromedioService.esCuadroDeHonor(p.getPromedio()) ? "(Cuadro de Honor)" : "";
            int horas = horasPorAlumno.getOrDefault(p.getId(), 0);
            System.out.printf(Locale.ROOT, "%-6d %-5s %-25s %-10.2f %-10s %-10s %s%n",
                p.getPuesto(), p.getId(), p.getNombreCompleto(), p.getPromedio(), beca, horas + "h", honor);
        }
        if (alumnosSinNotas > 0) {
            System.out.println(alumnosSinNotas + " alumno(s) sin notas no aparecen en el ranking.");
        }
    }

    public void mostrarFicha(Ficha f) {
        System.out.println("\n--- FICHA DE " + f.alumno().nombreCompleto().toUpperCase(Locale.ROOT) + " (" + f.alumno().id() + ") ---");
        if (f.notas().isEmpty()) {
            System.out.println("Sin notas registradas: no aparece en el ranking.");
            return;
        }
        System.out.printf(Locale.ROOT, "Promedio: %.2f   Puesto: %d   Creditos: %d   Horas semanales: %d   Reprobadas: %d%n",
            f.promedio(), f.puesto(), f.creditos(), f.horas(), f.reprobados());
        System.out.println("Beca: " + (f.beca() ? "SI" : String.format(Locale.ROOT, "NO (le faltan %.2f)", f.faltaBeca()))
            + "   Cuadro de honor: " + (f.honor() ? "SI" : String.format(Locale.ROOT, "NO (le faltan %.2f)", f.faltaHonor())));
        System.out.printf("%n%-5s %-25s %-9s %-6s %-8s%n", "ID", "Curso", "Creditos", "Nota", "Aporte");
        for (NotaFicha n : f.notas()) {
            System.out.printf(Locale.ROOT, "%-5s %-25s %-9d %-6.1f %-8.1f %s%n", n.cursoId(), n.curso(), n.creditos(),
                n.nota(), n.aporte(), n.aprobada() ? "" : "(reprobada)");
        }
    }

    // Lineas invalidas o referencias rotas encontradas al cargar/procesar.
    public void mostrarAdvertencias(List<String> advertencias) {
        if (advertencias.isEmpty()) return;
        System.out.println("\n--- ADVERTENCIAS (" + advertencias.size() + ") ---");
        advertencias.forEach(a -> System.out.println("  - " + a));
    }

    public void mostrarAcercaDe() {
        System.out.println("\n--- ACERCA DE ---");
        System.out.println(Proyecto.NOMBRE);
        System.out.println(Proyecto.INSTITUCION + " - " + Proyecto.ASIGNATURA);
        System.out.println("\nIntegrantes:");
        Proyecto.INTEGRANTES.forEach(nombre -> System.out.println("  " + nombre));
    }

    public void mensaje(String texto) {
        System.out.println(texto);
    }
}
