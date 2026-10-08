package com.notas.gui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Tabla de solo lectura, ordenable y filtrable. Si no hay filas muestra un mensaje en su lugar.
// Al recargar los datos conserva la fila seleccionada (la reconoce por su clave).
class Tabla extends JPanel {
    private final Modelo modelo;
    private final JTable tabla;
    private final TableRowSorter<Modelo> ordenador;
    private final CardLayout tarjetas = new CardLayout();
    private final Function<Object[], String> clave;
    private final List<Runnable> alCambiarFilas = new ArrayList<>();

    Tabla(String[] columnas, Class<?>[] clases, String mensajeVacio, Function<Object[], String> clave) {
        this.clave = clave;
        setLayout(tarjetas);
        setOpaque(false);

        modelo = new Modelo(columnas, clases);
        tabla = new JTable(modelo);
        Estilo.estilizarTabla(tabla);
        ordenador = new TableRowSorter<>(modelo);
        tabla.setRowSorter(ordenador);

        Tarjeta vacio = new Tarjeta(new GridBagLayout());
        JLabel mensaje = new JLabel("<html><div style='text-align:center'>"
            + Estilo.escaparHtml(mensajeVacio).replace("\n", "<br>") + "</div></html>", SwingConstants.CENTER);
        mensaje.setFont(Estilo.fuente(13, Font.PLAIN));
        mensaje.setForeground(Estilo.TEXTO_SUAVE);
        vacio.add(mensaje);

        add(Estilo.scroll(tabla), "tabla");
        add(vacio, "vacio");
    }

    void setFilas(List<Object[]> filas) {
        String seleccionada = claveSeleccionada();
        modelo.setFilas(filas);
        tarjetas.show(this, filas.isEmpty() ? "vacio" : "tabla");
        if (seleccionada != null) seleccionar(seleccionada);
        alCambiarFilas.forEach(Runnable::run);
    }

    Object[] seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.fila(tabla.convertRowIndexToModel(fila));
    }

    void seleccionar(String claveBuscada) {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (!clave.apply(modelo.fila(i)).equals(claveBuscada)) continue;
            int vista = tabla.convertRowIndexToView(i);
            if (vista >= 0) {
                tabla.setRowSelectionInterval(vista, vista);
                tabla.scrollRectToVisible(tabla.getCellRect(vista, 0, true));
            }
            return;
        }
    }

    private String claveSeleccionada() {
        Object[] fila = seleccionada();
        return fila == null ? null : clave.apply(fila);
    }

    void setFiltro(RowFilter<Object, Object> filtro) {
        ordenador.setRowFilter(filtro);
        alCambiarFilas.forEach(Runnable::run);
    }

    int visibles() {
        return tabla.getRowCount();
    }

    int total() {
        return modelo.getRowCount();
    }

    void alCambiarFilas(Runnable accion) {
        alCambiarFilas.add(accion);
    }

    void alCambiarSeleccion(Runnable accion) {
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) accion.run();
        });
    }

    void alDobleClic(Runnable accion) {
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0) accion.run();
            }
        });
    }

    // Clic derecho: selecciona la fila bajo el cursor y abre el menu.
    void menuContextual(JPopupMenu menu) {
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { mostrar(e); }
            @Override public void mouseReleased(MouseEvent e) { mostrar(e); }

            private void mostrar(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int fila = tabla.rowAtPoint(e.getPoint());
                if (fila < 0) return;
                tabla.setRowSelectionInterval(fila, fila);
                menu.show(tabla, e.getX(), e.getY());
            }
        });
    }

    // Atajo activo mientras el foco esta en la tabla (Enter, Supr...), sin afectar los campos de texto.
    void atajo(KeyStroke tecla, Runnable accion) {
        String nombre = "atajo-" + tecla;
        tabla.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(tecla, nombre);
        tabla.getActionMap().put(nombre, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (seleccionada() != null) accion.run();
            }
        });
    }

    void renderizador(int columna, TableCellRenderer renderizador) {
        tabla.getColumnModel().getColumn(columna).setCellRenderer(renderizador);
    }

    void decimales(int columna, int decimales) {
        renderizador(columna, new Estilo.RenderCelda(decimales));
    }

    // Columnas tipo "A10": se ordenan por numero, no alfabeticamente.
    void ordenarComoId(int... columnas) {
        for (int c : columnas) ordenador.setComparator(c, Estilo.COMPARADOR_ID);
    }

    void ordenInicial(int columna, SortOrder orden) {
        ordenador.setSortKeys(List.of(new RowSorter.SortKey(columna, orden)));
    }

    void anchos(int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private static class Modelo extends AbstractTableModel {
        private final String[] columnas;
        private final Class<?>[] clases;
        private List<Object[]> filas = List.of();

        Modelo(String[] columnas, Class<?>[] clases) {
            this.columnas = columnas;
            this.clases = clases;
        }

        void setFilas(List<Object[]> filas) {
            this.filas = new ArrayList<>(filas);
            fireTableDataChanged();
        }

        Object[] fila(int indice) {
            return filas.get(indice);
        }

        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return columnas.length; }
        @Override public String getColumnName(int columna) { return columnas[columna]; }
        @Override public Class<?> getColumnClass(int columna) { return clases[columna]; }
        @Override public Object getValueAt(int fila, int columna) { return filas.get(fila)[columna]; }
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    }
}
