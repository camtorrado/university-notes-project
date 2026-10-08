package com.notas.gui;

import com.notas.Proyecto;
import com.notas.service.PromedioService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

// Equivale a [9] Acerca de: proposito, reglas de calculo, archivos e integrantes.
class PanelAcercaDe extends Seccion {
    PanelAcercaDe(VentanaPrincipal ventana) {
        super(ventana, "Acerca de", Proyecto.INSTITUCION + " · " + Proyecto.ASIGNATURA);

        Tarjeta tarjeta = new Tarjeta(null);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(new EmptyBorder(24, 28, 24, 28));

        agregar(tarjeta, Estilo.texto(Proyecto.NOMBRE, 18, Font.BOLD, Estilo.TEXTO));
        tarjeta.add(Box.createVerticalStrut(8));
        agregar(tarjeta, Estilo.parrafo("Calcula el promedio ponderado por créditos de cada estudiante a partir de sus notas "
            + "por curso, y genera un ranking que identifica quiénes aplican a beca y quiénes están en el cuadro de honor.",
            620, Estilo.TEXTO_SUAVE));

        apartado(tarjeta, "Cómo se calcula",
            "Promedio = Σ (nota × créditos del curso) ÷ Σ créditos",
            "Beca: promedio ≥ " + Estilo.decimal(PromedioService.UMBRAL_BECA, 1),
            "Cuadro de honor: promedio ≥ " + Estilo.decimal(PromedioService.UMBRAL_HONOR, 1),
            "Carga horaria: suma de las horas semanales de los cursos en los que el alumno tiene nota",
            "Empates: comparten puesto (1, 2, 2, 4); se listan primero quienes cursaron más créditos",
            "Nota aprobatoria: " + Estilo.decimal(PromedioService.NOTA_APROBATORIA, 1) + " (informativa, no cambia beca ni honor)");

        apartado(tarjeta, "Archivos de la carpeta data/",
            "alumnos.csv — ID;NombreCompleto",
            "cursos.csv — ID;Nombre;N_creditos;M_horas",
            "notas.txt — AlumnoId;CursoId;Nota",
            "promedios.csv — salida generada con «Exportar CSV»");

        tarjeta.add(Box.createVerticalStrut(4));
        agregar(tarjeta, Estilo.parrafo("Ubicación: " + ventana.gestion.getDirDatos().toAbsolutePath(), 620, Estilo.DESHABILITADO));

        apartado(tarjeta, "Integrantes", Proyecto.INTEGRANTES.toArray(String[]::new));

        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.add(tarjeta, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(envoltura);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private static void apartado(JPanel tarjeta, String titulo, String... lineas) {
        tarjeta.add(Box.createVerticalStrut(22));
        agregar(tarjeta, Estilo.texto(titulo, 13, Font.BOLD, Estilo.TEXTO));
        tarjeta.add(Box.createVerticalStrut(6));
        for (String linea : lineas) {
            agregar(tarjeta, Estilo.texto("•  " + linea, 13, Font.PLAIN, Estilo.TEXTO_SUAVE));
            tarjeta.add(Box.createVerticalStrut(3));
        }
    }

    private static void agregar(JPanel tarjeta, JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        tarjeta.add(componente);
    }

    @Override
    void actualizar(Datos datos) {
        // Contenido fijo.
    }
}
