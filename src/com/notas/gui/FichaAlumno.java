package com.notas.gui;

import com.notas.service.GestionService.Ficha;
import com.notas.service.GestionService.NotaFicha;
import com.notas.service.PromedioService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SortOrder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

// Resumen de un alumno: promedio, puesto, estado frente a beca y honor, y el detalle de sus notas
// con lo que cada curso aporta al promedio ponderado.
class FichaAlumno extends JDialog {
    private static final String APROBADA = "Aprobada";

    FichaAlumno(VentanaPrincipal ventana, Ficha ficha, Runnable editar) {
        super(ventana, "Ficha de " + ficha.alumno().nombreCompleto(), true);

        JPanel raiz = new JPanel(new BorderLayout(0, 18));
        raiz.setBackground(Estilo.FONDO);
        raiz.setBorder(new EmptyBorder(22, 24, 18, 24));

        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.texto(ficha.alumno().nombreCompleto(), 20, Font.BOLD, Estilo.TEXTO));
        cabecera.add(Box.createVerticalStrut(4));
        cabecera.add(Estilo.texto(ficha.alumno().id() + "  ·  "
            + (ficha.puesto() == null ? "Sin notas: no aparece en el ranking" : "Puesto " + ficha.puesto() + " del ranking"),
            13, Font.PLAIN, Estilo.TEXTO_SUAVE));

        JPanel cifras = new JPanel(new GridLayout(1, 4, 10, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Promedio", ficha.notas().isEmpty() ? "—" : Estilo.decimal(ficha.promedio(), 2)));
        cifras.add(cifra("Créditos", String.valueOf(ficha.creditos())));
        cifras.add(cifra("Horas/sem", String.valueOf(ficha.horas())));
        cifras.add(cifra("Reprobadas", String.valueOf(ficha.reprobados())));

        JPanel estados = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        estados.setOpaque(false);
        if (!ficha.notas().isEmpty()) {
            estados.add(ficha.beca()
                ? Estilo.insignia("Aplica a beca", Estilo.EXITO, Estilo.EXITO_SUAVE)
                : Estilo.insignia("Le faltan " + Estilo.decimal(ficha.faltaBeca(), 2) + " para beca", Estilo.TEXTO_SUAVE, Estilo.HOVER));
            estados.add(ficha.honor()
                ? Estilo.insignia("Cuadro de honor", Estilo.HONOR, Estilo.HONOR_SUAVE)
                : Estilo.insignia("Le faltan " + Estilo.decimal(ficha.faltaHonor(), 2) + " para cuadro de honor", Estilo.TEXTO_SUAVE, Estilo.HOVER));
        }

        JPanel arriba = new JPanel(new BorderLayout(0, 14));
        arriba.setOpaque(false);
        arriba.add(cabecera, BorderLayout.NORTH);
        arriba.add(cifras, BorderLayout.CENTER);
        arriba.add(estados, BorderLayout.SOUTH);

        Tabla tabla = new Tabla(
            new String[]{"ID", "Curso", "Créditos", "Nota", "Aporte", "Estado"},
            new Class<?>[]{String.class, String.class, Integer.class, Double.class, Double.class, String.class},
            "Este alumno aún no tiene notas.", fila -> (String) fila[0]);
        tabla.ordenarComoId(0);
        tabla.ordenInicial(0, SortOrder.ASCENDING);
        tabla.renderizador(3, Estilo.renderNota());
        tabla.renderizador(5, new Estilo.RenderInsignia(v -> APROBADA.equals(v) ? null : new Color[]{Estilo.PELIGRO, Estilo.PELIGRO_SUAVE}));
        tabla.anchos(60, 220, 80, 70, 80, 110);
        List<Object[]> filas = new ArrayList<>();
        for (NotaFicha n : ficha.notas()) {
            filas.add(new Object[]{n.cursoId(), n.curso(), n.creditos(), n.nota(), n.aporte(),
                n.aprobada() ? APROBADA : "Reprobada"});
        }
        tabla.setFilas(filas);

        JLabel explicacion = Estilo.texto("Aporte = nota × créditos.  Promedio = Σ aportes ÷ Σ créditos.  Se aprueba con "
            + Estilo.decimal(PromedioService.NOTA_APROBATORIA, 1) + ".", 12, Font.PLAIN, Estilo.TEXTO_SUAVE);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(tabla, BorderLayout.CENTER);
        centro.add(explicacion, BorderLayout.SOUTH);

        BotonPlano verNotas = new BotonPlano("Ver notas", BotonPlano.Tipo.SECUNDARIO);
        BotonPlano botonEditar = new BotonPlano("Editar", BotonPlano.Tipo.SECUNDARIO);
        BotonPlano cerrar = new BotonPlano("Cerrar", BotonPlano.Tipo.PRIMARIO);
        verNotas.addActionListener(e -> {
            dispose();
            ventana.verNotas(ficha.alumno().id(), null);
        });
        botonEditar.addActionListener(e -> {
            dispose();
            editar.run();
        });
        cerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(verNotas);
        botones.add(botonEditar);
        botones.add(cerrar);

        raiz.add(arriba, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(cerrar);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(700, 580);
        setLocationRelativeTo(ventana);
    }

    private static JComponent cifra(String titulo, String valor) {
        Tarjeta tarjeta = new Tarjeta(null);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(new EmptyBorder(10, 14, 10, 14));
        tarjeta.add(Estilo.texto(titulo, 12, Font.PLAIN, Estilo.TEXTO_SUAVE));
        tarjeta.add(Box.createVerticalStrut(2));
        tarjeta.add(Estilo.texto(valor, 20, Font.BOLD, Estilo.TEXTO));
        return tarjeta;
    }
}
