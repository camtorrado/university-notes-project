package com.notas.view;

import com.notas.model.Alumno;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.service.PromedioService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleView {
    private final Scanner scanner = new Scanner(System.in);
    private final PromedioService promedioService = new PromedioService();

    public void mostrarMenu() {
        System.out.println("""

            C:\\> java main
            [1] Generar Archivos
            [2] Importar/Ver Base
            [3] Importar/Ver Salida
            [4] Descargar Salida
            [5] Salir
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

    // Pide la cantidad de alumnos a generar. Si la entrada no es un entero positivo, usa el valor por defecto.
    public int leerCantidadAlumnos(int porDefecto) {
        System.out.print("Cantidad de alumnos a generar (Enter para " + porDefecto + "): ");
        String entrada = scanner.nextLine().trim();
        if (entrada.isBlank()) return porDefecto;
        try {
            int cantidad = Integer.parseInt(entrada);
            return cantidad > 0 ? cantidad : porDefecto;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    public void mostrarAlumnos(Map<String, Alumno> alumnos) {
        System.out.println("\n--- ALUMNOS ---");
        alumnos.values().forEach(a ->
            System.out.printf("%-5s %s%n", a.id(), a.nombreCompleto()));
    }

    public void mostrarNotas(List<NotaCurso> notas) {
        System.out.println("\n--- NOTAS ---");
        notas.forEach(n ->
            System.out.printf("%-5s %-25s %.1f  %d creditos%n", n.alumnoId(), n.curso(), n.nota(), n.creditos()));
    }

    public void mostrarRanking(List<PromedioAlumno> ranking) {
        System.out.println("\n--- CUADRO DE HONOR / RANKING (ordenado por promedio) ---");
        System.out.printf("%-5s %-25s %-10s %s%n", "ID", "Nombre", "Promedio", "");
        for (PromedioAlumno p : ranking) {
            String honor = promedioService.esCuadroDeHonor(p) ? "(Cuadro de Honor)" : "";
            System.out.printf("%-5s %-25s %-10.1f %s%n", p.getId(), p.getNombreCompleto(), p.getPromedio(), honor);
        }
    }

    public void mensaje(String texto) {
        System.out.println(texto);
    }
}
