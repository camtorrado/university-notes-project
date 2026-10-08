package com.notas.gui;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

// Superficie blanca con borde suave y esquinas redondeadas.
class Tarjeta extends JPanel {

    Tarjeta(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 14, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Estilo.suavizar(g2);
        g2.setColor(Estilo.SUPERFICIE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.setColor(Estilo.BORDE);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}
