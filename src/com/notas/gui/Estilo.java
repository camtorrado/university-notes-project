package com.notas.gui;

import com.notas.service.PromedioService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

// Paleta, tipografia y renderizadores compartidos: todo el aspecto visual se decide aqui.
final class Estilo {
    static final Color FONDO = new Color(0xF5F6F8);
    static final Color SUPERFICIE = Color.WHITE;
    static final Color ENCABEZADO = new Color(0xFAFBFC);
    static final Color BORDE = new Color(0xE3E6EB);
    static final Color LINEA = new Color(0xEEF0F3);
    static final Color HOVER = new Color(0xF0F2F5);
    static final Color TEXTO = new Color(0x1E2430);
    static final Color TEXTO_SUAVE = new Color(0x6B7280);
    static final Color DESHABILITADO = new Color(0xB0B6BF);
    static final Color ACENTO = new Color(0x2F5BEA);
    static final Color ACENTO_HOVER = new Color(0x2449C4);
    static final Color ACENTO_SUAVE = new Color(0xEBF0FE);
    static final Color EXITO = new Color(0x167A3E);
    static final Color EXITO_SUAVE = new Color(0xE6F5EC);
    static final Color HONOR = new Color(0x8A5A00);
    static final Color HONOR_SUAVE = new Color(0xFFF1CC);
    static final Color PELIGRO = new Color(0xC92A2A);
    static final Color PELIGRO_SUAVE = new Color(0xFDECEC);
    static final Color AVISO = new Color(0xB45309);

    private Estilo() {
    }

    static Font fuente(float tamano, int estilo) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        return base.deriveFont(estilo, tamano);
    }

    static JLabel texto(String contenido, float tamano, int estilo, Color color) {
        JLabel etiqueta = new JLabel(contenido);
        etiqueta.setFont(fuente(tamano, estilo));
        etiqueta.setForeground(color);
        return etiqueta;
    }

    // Texto largo que se ajusta al ancho dado (los saltos de linea se respetan).
    static JLabel parrafo(String contenido, int ancho, Color color) {
        JLabel etiqueta = new JLabel("<html><body style='width:" + ancho + "px'>"
            + escaparHtml(contenido).replace("\n", "<br>") + "</body></html>");
        etiqueta.setFont(fuente(13, Font.PLAIN));
        etiqueta.setForeground(color);
        return etiqueta;
    }

    static String escaparHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Siempre con punto decimal, igual que en los archivos de /data.
    static String decimal(double valor, int decimales) {
        return String.format(Locale.ROOT, "%." + decimales + "f", valor);
    }

    static String plural(long cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    static void suavizar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Object pistas = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (pistas instanceof Map<?, ?> mapa) g2.addRenderingHints(mapa);
    }

    // Boton sin relleno, para acciones discretas (barra de estado).
    static JButton enlace(String contenido) {
        JButton boton = new JButton(contenido);
        boton.setUI(new BasicButtonUI());
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setOpaque(false);
        boton.setBorder(new EmptyBorder(2, 6, 2, 6));
        boton.setFont(fuente(12, Font.PLAIN));
        boton.setForeground(ACENTO);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    // Nota con un decimal; en rojo si esta por debajo de la nota aprobatoria.
    static RenderCelda renderNota() {
        return new RenderCelda(1, v -> v instanceof Double nota && !PromedioService.estaAprobada(nota)
            ? PELIGRO : null);
    }

    // Etiqueta suelta con fondo redondeado, como las insignias de la tabla.
    static JLabel insignia(String contenido, Color texto, Color fondo) {
        JLabel etiqueta = new JLabel(contenido) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                suavizar(g2);
                g2.setColor(fondo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        etiqueta.setFont(fuente(12, Font.BOLD));
        etiqueta.setForeground(texto);
        etiqueta.setBorder(new EmptyBorder(4, 10, 4, 10));
        return etiqueta;
    }

    static JScrollPane scroll(Component contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(new LineBorder(BORDE));
        scroll.getViewport().setBackground(SUPERFICIE);
        return scroll;
    }

    // "A10" va despues de "A2": compara el prefijo y luego el numero.
    static final Comparator<Object> COMPARADOR_ID = (a, b) -> {
        String x = String.valueOf(a);
        String y = String.valueOf(b);
        String prefijoX = x.replaceAll("\\d+$", "");
        String prefijoY = y.replaceAll("\\d+$", "");
        int porPrefijo = prefijoX.compareTo(prefijoY);
        if (porPrefijo != 0) return porPrefijo;
        try {
            return Long.compare(Long.parseLong(x.substring(prefijoX.length())), Long.parseLong(y.substring(prefijoY.length())));
        } catch (NumberFormatException e) {
            return x.compareTo(y);
        }
    };

    static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(34);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(LINEA);
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setFont(fuente(13, Font.PLAIN));
        tabla.setForeground(TEXTO);
        tabla.setBackground(SUPERFICIE);
        tabla.setSelectionBackground(ACENTO_SUAVE);
        tabla.setSelectionForeground(TEXTO);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setBorder(BorderFactory.createEmptyBorder());

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setDefaultRenderer(new RenderEncabezado());
        encabezado.setPreferredSize(new Dimension(0, 34));
        encabezado.setBackground(ENCABEZADO);

        // Swing trae renderizadores propios (alineados a la derecha) para Number, Double y Float.
        for (Class<?> clase : List.of(Object.class, Number.class, Double.class, Float.class)) {
            tabla.setDefaultRenderer(clase, new RenderCelda(1));
        }
    }

    // Celda con margen; null se muestra como guion y los decimales con punto.
    // colorTexto permite resaltar valores (por ejemplo notas reprobadas); null = color normal.
    static class RenderCelda extends DefaultTableCellRenderer {
        private final int decimales;
        private final Function<Object, Color> colorTexto;

        RenderCelda(int decimales) {
            this(decimales, v -> null);
        }

        RenderCelda(int decimales, Function<Object, Color> colorTexto) {
            this.decimales = decimales;
            this.colorTexto = colorTexto;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(LEADING);
            setBackground(seleccionada ? ACENTO_SUAVE : SUPERFICIE);
            Color resaltado = valor == null ? null : colorTexto.apply(valor);
            setForeground(valor == null ? TEXTO_SUAVE : resaltado != null ? resaltado : TEXTO);
            setFont(fuente(13, resaltado != null ? Font.BOLD : Font.PLAIN));
            if (valor == null) setText("—");
            else if (valor instanceof Double d) setText(decimal(d, decimales));
            return this;
        }
    }

    // Etiqueta con fondo redondeado; si la funcion devuelve null se dibuja como texto suave.
    // La funcion devuelve {color de texto, color de fondo}.
    static class RenderInsignia extends DefaultTableCellRenderer {
        private final Function<Object, Color[]> colores;
        private Color[] actual;

        RenderInsignia(Function<Object, Color[]> colores) {
            this.colores = colores;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna);
            actual = colores.apply(valor);
            setBackground(seleccionada ? ACENTO_SUAVE : SUPERFICIE);
            setForeground(actual == null ? TEXTO_SUAVE : actual[0]);
            setFont(fuente(12, actual == null ? Font.PLAIN : Font.BOLD));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            String contenido = getText();
            if (contenido != null && !contenido.isEmpty()) {
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = 12;
                if (actual != null) {
                    int alto = fm.getHeight() + 4;
                    g2.setColor(actual[1]);
                    g2.fillRoundRect(x, (getHeight() - alto) / 2, fm.stringWidth(contenido) + 16, alto, alto, alto);
                    x += 8;
                }
                g2.setColor(getForeground());
                g2.drawString(contenido, x, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    // Encabezado discreto con flecha en la columna por la que se esta ordenando.
    static class RenderEncabezado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            super.getTableCellRendererComponent(tabla, valor, false, false, fila, columna);
            setFont(fuente(12, Font.BOLD));
            setForeground(TEXTO_SUAVE);
            setBackground(ENCABEZADO);
            setOpaque(true);
            setHorizontalAlignment(LEADING);
            setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDE), new EmptyBorder(0, 12, 0, 12)));

            String flecha = "";
            RowSorter<?> ordenador = tabla.getRowSorter();
            if (ordenador != null) {
                List<? extends RowSorter.SortKey> claves = ordenador.getSortKeys();
                if (!claves.isEmpty() && claves.get(0).getColumn() == tabla.convertColumnIndexToModel(columna)) {
                    flecha = claves.get(0).getSortOrder() == SortOrder.DESCENDING ? "  ↓" : "  ↑";
                }
            }
            setText(valor + flecha);
            return this;
        }
    }
}
