package com.notas.gui;

import com.notas.model.PromedioAlumno;
import com.notas.service.GestionService.EstadoSalida;
import com.notas.service.PromedioService;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Equivale a [3] Ver Salida, [4] Descargar Salida y [5] Limpiar Salida de la consola.
class PanelResultados extends Seccion {
    private static final String SI = "Sí";
    private static final String HONOR = "Cuadro de honor";
    private static final String[] FILTROS = {"Todos", "Aplican a beca", "Cuadro de honor"};

    private final JLabel evaluados;
    private final JLabel promedioGrupo;
    private final JLabel becas;
    private final JLabel honor;
    private final GraficoDistribucion grafico = new GraficoDistribucion();
    private final JLabel salida = Estilo.texto("", 12, Font.PLAIN, Estilo.TEXTO_SUAVE);
    private final JButton mostrarCarpeta = Estilo.enlace("Mostrar en carpeta");
    private final JLabel sinNotas = Estilo.texto("", 12, Font.PLAIN, Estilo.TEXTO_SUAVE);
    private final JComboBox<String> filtro = new JComboBox<>(FILTROS);
    private final CampoBusqueda busqueda;
    private final Tabla tabla;
    private final BotonPlano limpiar;
    private final BotonPlano exportar;

    PanelResultados(VentanaPrincipal ventana) {
        super(ventana, "Resultados", "Ranking por promedio ponderado:  Σ (nota × créditos) ÷ Σ créditos");

        limpiar = accion(new BotonPlano("Limpiar salida", BotonPlano.Tipo.SECUNDARIO));
        exportar = accion(new BotonPlano("Exportar CSV", BotonPlano.Tipo.PRIMARIO));
        limpiar.addActionListener(e -> limpiarSalida());
        exportar.addActionListener(e -> exportar());
        exportar.setToolTipText("Guarda el ranking actual en data/promedios.csv");
        mostrarCarpeta.addActionListener(e -> abrirCarpeta());

        JPanel resumen = new JPanel(new GridBagLayout());
        resumen.setOpaque(false);
        evaluados = indicador(resumen, "Alumnos evaluados", "Con al menos una nota");
        promedioGrupo = indicador(resumen, "Promedio del grupo", "Media de los promedios");
        becas = indicador(resumen, "Aplican a beca", "Promedio ≥ " + Estilo.decimal(PromedioService.UMBRAL_BECA, 1));
        honor = indicador(resumen, "Cuadro de honor", "Promedio ≥ " + Estilo.decimal(PromedioService.UMBRAL_HONOR, 1));
        Tarjeta tarjetaGrafico = new Tarjeta(new BorderLayout(0, 6));
        tarjetaGrafico.add(Estilo.texto("Distribución de promedios", 12, Font.PLAIN, Estilo.TEXTO_SUAVE), BorderLayout.NORTH);
        tarjetaGrafico.add(grafico, BorderLayout.CENTER);
        agregarAlResumen(resumen, tarjetaGrafico, 1.7);

        tabla = new Tabla(
            new String[]{"Puesto", "ID", "Alumno", "Promedio", "Créditos", "Horas/sem", "Beca", "Distinción"},
            new Class<?>[]{Integer.class, String.class, String.class, Double.class, Integer.class, Integer.class, String.class, String.class},
            "Todavía no hay notas para calcular el ranking.\nRegistra alumnos, cursos y notas, o usa «Generar datos de ejemplo».",
            fila -> (String) fila[1]);
        tabla.ordenarComoId(1);
        tabla.ordenInicial(0, SortOrder.ASCENDING);
        tabla.decimales(3, 2);
        tabla.renderizador(6, new Estilo.RenderInsignia(v -> SI.equals(v) ? new Color[]{Estilo.EXITO, Estilo.EXITO_SUAVE} : null));
        tabla.renderizador(7, new Estilo.RenderInsignia(v -> HONOR.equals(v) ? new Color[]{Estilo.HONOR, Estilo.HONOR_SUAVE} : null));
        tabla.anchos(80, 60, 200, 90, 80, 100, 80, 150);

        busqueda = new CampoBusqueda("Buscar por ID o nombre", texto -> aplicarFiltro());
        filtro.addActionListener(e -> aplicarFiltro());
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controles.setOpaque(false);
        controles.add(busqueda);
        controles.add(filtro);
        atajoVentana(conModificador(KeyEvent.VK_F), busqueda::requestFocusInWindow);

        JPanel estadoSalida = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        estadoSalida.setOpaque(false);
        estadoSalida.add(salida);
        estadoSalida.add(mostrarCarpeta);

        JPanel centro = new JPanel(new BorderLayout(0, 18));
        centro.setOpaque(false);
        centro.add(resumen, BorderLayout.NORTH);
        centro.add(cuerpo(barra(controles, conteo(tabla, "alumno", "alumnos")), tabla), BorderLayout.CENTER);
        centro.add(barra(estadoSalida, sinNotas), BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
    }

    private static JLabel indicador(JPanel resumen, String titulo, String detalle) {
        Tarjeta tarjeta = new Tarjeta(null);
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        JLabel valor = Estilo.texto("—", 24, Font.BOLD, Estilo.TEXTO);
        tarjeta.add(Estilo.texto(titulo, 12, Font.PLAIN, Estilo.TEXTO_SUAVE));
        tarjeta.add(Box.createVerticalStrut(4));
        tarjeta.add(valor);
        tarjeta.add(Box.createVerticalStrut(2));
        tarjeta.add(Estilo.texto(detalle, 11, Font.PLAIN, Estilo.DESHABILITADO));
        agregarAlResumen(resumen, tarjeta, 1);
        return valor;
    }

    private static void agregarAlResumen(JPanel resumen, JPanel tarjeta, double peso) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = peso;
        c.weighty = 1;
        c.insets = new Insets(0, resumen.getComponentCount() == 0 ? 0 : 12, 0, 0);
        resumen.add(tarjeta, c);
    }

    @Override
    void actualizar(Datos datos) {
        List<PromedioAlumno> ranking = datos.ranking();
        List<Object[]> filas = new ArrayList<>();
        for (PromedioAlumno p : ranking) {
            filas.add(new Object[]{
                p.getPuesto(), p.getId(), p.getNombreCompleto(), p.getPromedio(), p.getSumaCreditos(),
                datos.horasPorAlumno().getOrDefault(p.getId(), 0),
                PromedioService.aplicaBeca(p.getPromedio()) ? SI : "No",
                PromedioService.esCuadroDeHonor(p.getPromedio()) ? HONOR : ""
            });
        }
        tabla.setFilas(filas);

        List<Double> promedios = ranking.stream().map(PromedioAlumno::getPromedio).toList();
        evaluados.setText(String.valueOf(ranking.size()));
        promedioGrupo.setText(ranking.isEmpty() ? "—"
            : Estilo.decimal(promedios.stream().mapToDouble(Double::doubleValue).average().orElse(0), 2));
        becas.setText(String.valueOf(promedios.stream().filter(PromedioService::aplicaBeca).count()));
        honor.setText(String.valueOf(promedios.stream().filter(PromedioService::esCuadroDeHonor).count()));
        grafico.setPromedios(promedios);

        sinNotas.setText(datos.alumnosSinNotas() == 0 ? ""
            : Estilo.plural(datos.alumnosSinNotas(), "alumno sin notas no aparece", "alumnos sin notas no aparecen")
                + " en el ranking");

        switch (datos.estadoSalida()) {
            case NO_GENERADA -> {
                salida.setText("promedios.csv aún no se ha exportado");
                salida.setForeground(Estilo.TEXTO_SUAVE);
            }
            case AL_DIA -> {
                salida.setText("promedios.csv exportado y al día");
                salida.setForeground(Estilo.EXITO);
            }
            case DESACTUALIZADA -> {
                salida.setText("promedios.csv está desactualizado: los datos cambiaron después de exportar");
                salida.setForeground(Estilo.AVISO);
            }
        }
        boolean hayArchivo = datos.estadoSalida() != EstadoSalida.NO_GENERADA;
        mostrarCarpeta.setVisible(hayArchivo && Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));
        limpiar.setEnabled(hayArchivo);
        exportar.setEnabled(!ranking.isEmpty());
    }

    // Combina la busqueda por texto con el filtro de beca / cuadro de honor.
    private void aplicarFiltro() {
        String texto = busqueda.getText().trim();
        int eleccion = filtro.getSelectedIndex();
        List<RowFilter<Object, Object>> filtros = new ArrayList<>();
        if (!texto.isEmpty()) filtros.add(RowFilter.regexFilter("(?iu)" + Pattern.quote(texto), 1, 2));
        if (eleccion == 1) filtros.add(RowFilter.regexFilter("^" + SI + "$", 6));
        if (eleccion == 2) filtros.add(RowFilter.regexFilter("^" + HONOR + "$", 7));
        tabla.setFiltro(filtros.isEmpty() ? null : RowFilter.andFilter(filtros));
    }

    private void exportar() {
        ventana.intentar(() -> {
            Path ruta = ventana.gestion.exportarRanking();
            ventana.estado("Ranking exportado en " + ruta.toAbsolutePath());
        });
    }

    private void abrirCarpeta() {
        try {
            Desktop.getDesktop().open(ventana.gestion.getDirDatos().toAbsolutePath().toFile());
        } catch (IOException | UnsupportedOperationException e) {
            ventana.mostrarError("No se pudo abrir la carpeta", e.getMessage());
        }
    }

    private void limpiarSalida() {
        if (!ventana.confirmar("Limpiar salida",
                "Se eliminará data/promedios.csv.\n\nEl ranking en pantalla no cambia: siempre se calcula desde alumnos.csv, cursos.csv y notas.txt.",
                "Eliminar archivo")) {
            return;
        }
        ventana.intentar(() -> ventana.estado(ventana.gestion.limpiarSalida()
            ? "Salida eliminada." : "No había ninguna salida generada."));
    }
}
