package com.notas.gui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

// Pantalla de la ventana: titulo, descripcion y acciones arriba; el contenido lo pone cada subclase.
abstract class Seccion extends JPanel {
    protected final VentanaPrincipal ventana;
    private final String nombre;
    private final JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    Seccion(VentanaPrincipal ventana, String nombre, String descripcion) {
        super(new BorderLayout(0, 18));
        this.ventana = ventana;
        this.nombre = nombre;
        setBackground(Estilo.FONDO);
        setBorder(new EmptyBorder(26, 30, 22, 30));

        // Titulo y acciones en una fila; la descripcion debajo, a todo el ancho.
        acciones.setOpaque(false);
        JPanel filaTitulo = new JPanel(new BorderLayout(16, 0));
        filaTitulo.setOpaque(false);
        filaTitulo.add(Estilo.texto(nombre, 22, Font.BOLD, Estilo.TEXTO), BorderLayout.CENTER);
        filaTitulo.add(acciones, BorderLayout.EAST);

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        cabecera.add(filaTitulo, BorderLayout.NORTH);
        cabecera.add(Estilo.texto(descripcion, 13, Font.PLAIN, Estilo.TEXTO_SUAVE), BorderLayout.CENTER);
        add(cabecera, BorderLayout.NORTH);
    }

    String nombre() {
        return nombre;
    }

    abstract void actualizar(Datos datos);

    protected <T extends JComponent> T accion(T boton) {
        acciones.add(boton);
        return boton;
    }

    // Ctrl+tecla en Windows/Linux, Cmd+tecla en macOS.
    protected static KeyStroke conModificador(int tecla) {
        return KeyStroke.getKeyStroke(tecla, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    protected static final KeyStroke SUPRIMIR = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
    protected static final KeyStroke RETROCESO = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
    protected static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    // Atajo de toda la ventana; solo responde mientras esta seccion es la visible.
    protected void atajoVentana(KeyStroke tecla, Runnable accion) {
        String nombre = "atajo-" + tecla;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(tecla, nombre);
        getActionMap().put(nombre, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) accion.run();
            }
        });
    }

    // Menu emergente; null en la lista agrega un separador.
    protected static JPopupMenu menu(Object... textosYAcciones) {
        JPopupMenu menu = new JPopupMenu();
        for (int i = 0; i < textosYAcciones.length; i++) {
            if (textosYAcciones[i] == null) {
                menu.addSeparator();
                continue;
            }
            JMenuItem item = new JMenuItem((String) textosYAcciones[i]);
            Runnable accion = (Runnable) textosYAcciones[++i];
            item.addActionListener(e -> accion.run());
            menu.add(item);
        }
        return menu;
    }

    // Fila sobre la tabla: controles a la izquierda, informacion a la derecha.
    protected static JPanel barra(JComponent izquierda, JComponent derecha) {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setOpaque(false);
        if (izquierda != null) barra.add(izquierda, BorderLayout.WEST);
        if (derecha != null) barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    protected static JPanel cuerpo(JComponent arriba, JComponent centro) {
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder());
        if (arriba != null) cuerpo.add(arriba, BorderLayout.NORTH);
        cuerpo.add(centro, BorderLayout.CENTER);
        return cuerpo;
    }

    // "8 alumnos" o, si hay un filtro activo, "3 de 8 alumnos".
    protected static JLabel conteo(Tabla tabla, String singular, String plural) {
        JLabel etiqueta = Estilo.texto("", 12, Font.PLAIN, Estilo.TEXTO_SUAVE);
        tabla.alCambiarFilas(() -> etiqueta.setText(tabla.visibles() == tabla.total()
            ? Estilo.plural(tabla.total(), singular, plural)
            : tabla.visibles() + " de " + Estilo.plural(tabla.total(), singular, plural)));
        return etiqueta;
    }

    // Busqueda sin distinguir mayusculas en las columnas indicadas. La 'u' hace que tambien
    // aplique a tildes y ñ ("MÉNDEZ" encuentra "Méndez").
    protected static CampoBusqueda busqueda(Tabla tabla, String indicacion, int... columnas) {
        return new CampoBusqueda(indicacion, texto -> tabla.setFiltro(texto.isEmpty()
            ? null
            : RowFilter.regexFilter("(?iu)" + Pattern.quote(texto), columnas)));
    }
}
