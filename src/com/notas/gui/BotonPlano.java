package com.notas.gui;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

// Boton plano con esquinas redondeadas, igual en cualquier sistema operativo.
class BotonPlano extends JButton {
    enum Tipo { PRIMARIO, SECUNDARIO, PELIGRO }

    private final Tipo tipo;

    BotonPlano(String texto, Tipo tipo) {
        super(texto);
        this.tipo = tipo;
        setUI(new BasicButtonUI());
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(new EmptyBorder(8, 14, 8, 14));
        setFont(Estilo.fuente(13, tipo == Tipo.PRIMARIO ? Font.BOLD : Font.PLAIN));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Estilo.suavizar(g2);
        boolean activo = isEnabled();
        boolean resaltado = activo && (getModel().isRollover() || getModel().isPressed());

        Color fondo;
        Color borde;
        Color texto;
        switch (tipo) {
            case PRIMARIO -> {
                fondo = !activo ? Estilo.DESHABILITADO : resaltado ? Estilo.ACENTO_HOVER : Estilo.ACENTO;
                borde = null;
                texto = Color.WHITE;
            }
            case PELIGRO -> {
                fondo = resaltado ? Estilo.PELIGRO_SUAVE : Estilo.SUPERFICIE;
                borde = Estilo.BORDE;
                texto = activo ? Estilo.PELIGRO : Estilo.DESHABILITADO;
            }
            default -> {
                fondo = resaltado ? Estilo.HOVER : Estilo.SUPERFICIE;
                borde = Estilo.BORDE;
                texto = activo ? Estilo.TEXTO : Estilo.DESHABILITADO;
            }
        }

        int ancho = getWidth() - 1;
        int alto = getHeight() - 1;
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, ancho, alto, 10, 10);
        if (borde != null) {
            g2.setColor(borde);
            g2.drawRoundRect(0, 0, ancho, alto, 10, 10);
        }

        g2.setFont(getFont());
        g2.setColor(texto);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
            (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
