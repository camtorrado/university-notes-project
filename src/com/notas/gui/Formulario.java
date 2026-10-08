package com.notas.gui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

// Campos en dos columnas (etiqueta | control) para usar dentro de un dialogo.
class Formulario extends JPanel {
    private int filas = 0;

    Formulario() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    Formulario campo(String etiqueta, JComponent control) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = filas++;
        c.insets = new Insets(5, 0, 5, 12);
        c.anchor = GridBagConstraints.LINE_START;
        JLabel texto = Estilo.texto(etiqueta, 13, Font.PLAIN, Estilo.TEXTO_SUAVE);
        add(texto, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        control.setPreferredSize(new Dimension(280, control.getPreferredSize().height));
        add(control, c);
        return this;
    }

    Formulario nota(String texto, Color color) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = filas++;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 0, 0);
        c.anchor = GridBagConstraints.LINE_START;
        add(Estilo.parrafo(texto, 360, color), c);
        return this;
    }

    // JOptionPane enfoca el boton por defecto; esto deja el cursor en el primer campo.
    static void enfocarAlAbrir(JComponent control) {
        control.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent e) {
                control.requestFocusInWindow();
                control.removeAncestorListener(this);
            }
            @Override public void ancestorRemoved(AncestorEvent e) { }
            @Override public void ancestorMoved(AncestorEvent e) { }
        });
    }
}
