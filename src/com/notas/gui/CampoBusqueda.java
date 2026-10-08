package com.notas.gui;

import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Consumer;

// Campo de texto con indicacion gris mientras esta vacio; avisa en cada tecla.
class CampoBusqueda extends JTextField {
    private final String indicacion;

    CampoBusqueda(String indicacion, Consumer<String> alCambiar) {
        this.indicacion = indicacion;
        setFont(Estilo.fuente(13, Font.PLAIN));
        setForeground(Estilo.TEXTO);
        setBackground(Estilo.SUPERFICIE);
        setBorder(new CompoundBorder(new LineBorder(Estilo.BORDE, 1, true), new EmptyBorder(6, 10, 6, 10)));
        setPreferredSize(new Dimension(260, 34));
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { alCambiar.accept(getText().trim()); }
            @Override public void removeUpdate(DocumentEvent e) { alCambiar.accept(getText().trim()); }
            @Override public void changedUpdate(DocumentEvent e) { alCambiar.accept(getText().trim()); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!getText().isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Estilo.suavizar(g2);
        g2.setColor(Estilo.DESHABILITADO);
        g2.setFont(getFont());
        g2.drawString(indicacion, getInsets().left,
            (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent());
        g2.dispose();
    }
}
