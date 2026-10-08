package com.notas.gui;

import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

// Entrada de la barra lateral; la seleccionada queda resaltada con el color de acento.
class ItemNavegacion extends JToggleButton {

    ItemNavegacion(String texto) {
        super(texto);
        setUI(new BasicToggleButtonUI());
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(new EmptyBorder(0, 12, 0, 12));
        setFont(Estilo.fuente(13, Font.PLAIN));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        setPreferredSize(new Dimension(180, 36));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Estilo.suavizar(g2);
        if (isSelected() || getModel().isRollover()) {
            g2.setColor(isSelected() ? Estilo.ACENTO_SUAVE : Estilo.HOVER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
        g2.setFont(Estilo.fuente(13, isSelected() ? Font.BOLD : Font.PLAIN));
        g2.setColor(isSelected() ? Estilo.ACENTO : Estilo.TEXTO);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
