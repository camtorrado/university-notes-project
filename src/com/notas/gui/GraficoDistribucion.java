package com.notas.gui;

import com.notas.service.PromedioService;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

// Barras con cuantos alumnos caen en cada rango de promedio. Los colores siguen las insignias:
// verde = aplica a beca, dorado = cuadro de honor.
class GraficoDistribucion extends JComponent {
    private static final String[] RANGOS = {"< 3.0", "3.0–3.4", "3.5–3.9", "4.0–4.4", "≥ 4.5"};
    private static final double[] LIMITES = {3.0, 3.5, PromedioService.UMBRAL_BECA, PromedioService.UMBRAL_HONOR};
    private static final Color[] COLORES = {
        Estilo.DESHABILITADO, Estilo.ACENTO, Estilo.ACENTO, Estilo.EXITO, Estilo.HONOR
    };

    private final int[] conteos = new int[RANGOS.length];

    GraficoDistribucion() {
        setPreferredSize(new Dimension(260, 70));
    }

    void setPromedios(List<Double> promedios) {
        Arrays.fill(conteos, 0);
        for (double promedio : promedios) {
            int rango = 0;
            while (rango < LIMITES.length && promedio >= LIMITES[rango]) rango++;
            conteos[rango]++;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Estilo.suavizar(g2);
        g2.setFont(Estilo.fuente(10, Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();

        int maximo = 1;
        for (int c : conteos) maximo = Math.max(maximo, c);

        int columnas = RANGOS.length;
        int anchoColumna = getWidth() / columnas;
        int anchoBarra = Math.min(28, anchoColumna - 10);
        int altoEtiqueta = fm.getHeight();
        int altoBarras = getHeight() - altoEtiqueta * 2 - 2;

        for (int i = 0; i < columnas; i++) {
            int centro = i * anchoColumna + anchoColumna / 2;
            int alto = conteos[i] == 0 ? 2 : Math.max(4, altoBarras * conteos[i] / maximo);
            int y = altoEtiqueta + altoBarras - alto;

            g2.setColor(conteos[i] == 0 ? Estilo.LINEA : COLORES[i]);
            g2.fillRoundRect(centro - anchoBarra / 2, y, anchoBarra, alto, 4, 4);

            g2.setColor(Estilo.TEXTO);
            String cantidad = String.valueOf(conteos[i]);
            g2.drawString(cantidad, centro - fm.stringWidth(cantidad) / 2, y - 3);

            g2.setColor(Estilo.TEXTO_SUAVE);
            g2.drawString(RANGOS[i], centro - fm.stringWidth(RANGOS[i]) / 2, getHeight() - fm.getDescent());
        }
        g2.dispose();
    }
}
