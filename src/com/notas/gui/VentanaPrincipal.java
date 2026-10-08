package com.notas.gui;

import com.notas.Proyecto;
import com.notas.service.GestionService;
import com.notas.service.OperacionInvalidaException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

// Ventana de la version grafica: barra lateral con las secciones, contenido al centro y barra
// de estado abajo. Despues de cada accion relee /data y refresca todas las secciones.
public class VentanaPrincipal extends JFrame {
    private static final int ALUMNOS_POR_DEFECTO = 8;
    private static final int CURSOS_POR_DEFECTO = 6;

    @FunctionalInterface
    interface Accion {
        void ejecutar() throws IOException;
    }

    final GestionService gestion;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final List<Seccion> secciones = new ArrayList<>();
    private final Map<String, ItemNavegacion> navegacion = new LinkedHashMap<>();
    private final ButtonGroup grupoNavegacion = new ButtonGroup();
    private final JLabel estado = Estilo.texto(" ", 12, Font.PLAIN, Estilo.TEXTO_SUAVE);
    private final JButton botonAdvertencias = Estilo.enlace("");
    private final PanelNotas panelNotas;
    private Datos datos = Datos.vacio();

    public VentanaPrincipal(GestionService gestion) {
        super(Proyecto.NOMBRE);
        this.gestion = gestion;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 620));
        setSize(1200, 740);
        setLocationRelativeTo(null);
        contenido.setBackground(Estilo.FONDO);

        panelNotas = new PanelNotas(this);
        registrar(new PanelResultados(this));
        registrar(new PanelAlumnos(this));
        registrar(new PanelCursos(this));
        registrar(panelNotas);
        registrar(new PanelAcercaDe(this));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilo.FONDO);
        raiz.add(crearBarraLateral(), BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);
        raiz.add(crearBarraEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);

        recargar();
        mostrar("Resultados");
        estado("Datos leídos de " + gestion.getDirDatos() + "/");
    }

    private void registrar(Seccion seccion) {
        secciones.add(seccion);
        contenido.add(seccion, seccion.nombre());
    }

    // ---------- Estructura ----------

    private JComponent crearBarraLateral() {
        JPanel barra = new JPanel();
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBackground(Estilo.SUPERFICIE);
        barra.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, Estilo.BORDE), new EmptyBorder(22, 14, 18, 14)));
        barra.setPreferredSize(new Dimension(224, 0));

        agregarIzquierda(barra, Estilo.texto(Proyecto.NOMBRE, 15, Font.BOLD, Estilo.TEXTO));
        barra.add(Box.createVerticalStrut(2));
        agregarIzquierda(barra, Estilo.texto("Promedios ponderados", 12, Font.PLAIN, Estilo.TEXTO_SUAVE));
        barra.add(Box.createVerticalStrut(26));

        agregarGrupo(barra, "ANALISIS");
        agregarItem(barra, "Resultados");
        barra.add(Box.createVerticalStrut(18));
        agregarGrupo(barra, "DATOS");
        agregarItem(barra, "Alumnos");
        agregarItem(barra, "Cursos");
        agregarItem(barra, "Notas");

        barra.add(Box.createVerticalGlue());
        BotonPlano generar = new BotonPlano("Generar datos de ejemplo", BotonPlano.Tipo.SECUNDARIO);
        generar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        generar.addActionListener(e -> generarDatosDeEjemplo());
        agregarIzquierda(barra, generar);
        barra.add(Box.createVerticalStrut(8));
        agregarItem(barra, "Acerca de");
        return barra;
    }

    private void agregarGrupo(JPanel barra, String titulo) {
        JLabel etiqueta = Estilo.texto(titulo, 11, Font.BOLD, Estilo.DESHABILITADO);
        etiqueta.setBorder(new EmptyBorder(0, 12, 6, 0));
        agregarIzquierda(barra, etiqueta);
    }

    private void agregarItem(JPanel barra, String seccion) {
        ItemNavegacion item = new ItemNavegacion(seccion);
        item.addActionListener(e -> mostrar(seccion));
        grupoNavegacion.add(item);
        navegacion.put(seccion, item);
        barra.add(item);
        barra.add(Box.createVerticalStrut(2));
    }

    private static void agregarIzquierda(JPanel barra, JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(componente);
    }

    private JComponent crearBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Estilo.SUPERFICIE);
        barra.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Estilo.BORDE), new EmptyBorder(6, 16, 6, 10)));
        barra.add(estado, BorderLayout.CENTER);

        JButton recargar = Estilo.enlace("Recargar archivos");
        recargar.setToolTipText("Vuelve a leer alumnos.csv, cursos.csv y notas.txt (por si se editaron fuera del programa)");
        recargar.addActionListener(e -> {
            recargar();
            estado("Archivos recargados.");
        });
        botonAdvertencias.addActionListener(e -> mostrarAdvertencias());

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        derecha.setOpaque(false);
        derecha.add(botonAdvertencias);
        derecha.add(recargar);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    // ---------- Navegacion y datos ----------

    void mostrar(String seccion) {
        tarjetas.show(contenido, seccion);
        ItemNavegacion item = navegacion.get(seccion);
        if (item != null) item.setSelected(true);
    }

    // Abre la seccion Notas filtrada por alumno y/o curso (null = todos).
    void verNotas(String alumnoId, String cursoId) {
        panelNotas.filtrar(alumnoId, cursoId);
        mostrar("Notas");
    }

    // Relee los archivos de /data y refresca todas las secciones.
    void recargar() {
        try {
            GestionService.Base base = gestion.cargarBase();
            datos = Datos.de(base, gestion.calcularRanking(base), gestion.estadoSalida());
        } catch (IOException e) {
            datos = Datos.vacio();
            mostrarError("Error de archivo", "No se pudieron leer los archivos de /data: " + e.getMessage());
        }
        for (Seccion seccion : secciones) seccion.actualizar(datos);

        int cantidad = datos.advertencias().size();
        botonAdvertencias.setText(cantidad == 0 ? "Sin advertencias" : Estilo.plural(cantidad, "advertencia", "advertencias"));
        botonAdvertencias.setForeground(cantidad == 0 ? Estilo.TEXTO_SUAVE : Estilo.AVISO);
    }

    Datos datos() {
        return datos;
    }

    // Ejecuta una accion sobre los datos y refresca. Devuelve false solo si se incumplio una regla
    // (el usuario puede corregir y reintentar); un error de archivo se informa y cierra el intento.
    boolean intentar(Accion accion) {
        try {
            accion.ejecutar();
            recargar();
            return true;
        } catch (OperacionInvalidaException e) {
            mostrarError("No se pudo completar", e.getMessage());
            return false;
        } catch (IOException e) {
            mostrarError("Error de archivo", e.getMessage());
            recargar();
            return true;
        }
    }

    void estado(String mensaje) {
        estado.setText(mensaje);
    }

    // ---------- Dialogos ----------

    void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, Estilo.parrafo(mensaje, 340, Estilo.TEXTO), titulo, JOptionPane.ERROR_MESSAGE);
    }

    void mostrarInformacion(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(this, Estilo.parrafo(mensaje, 340, Estilo.TEXTO), titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    // Por seguridad, el boton por defecto es Cancelar.
    boolean confirmar(String titulo, String mensaje, String textoAccion) {
        String[] opciones = {textoAccion, "Cancelar"};
        int eleccion = JOptionPane.showOptionDialog(this, Estilo.parrafo(mensaje, 340, Estilo.TEXTO), titulo,
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[1]);
        return eleccion == 0;
    }

    // Muestra el formulario hasta que se guarde bien o se cancele: si una regla falla, el usuario
    // ve el motivo y vuelve al formulario con lo que ya habia escrito.
    void formulario(String titulo, JComponent formulario, String textoAccion, BooleanSupplier guardar) {
        String[] opciones = {textoAccion, "Cancelar"};
        while (true) {
            int eleccion = JOptionPane.showOptionDialog(this, formulario, titulo,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
            if (eleccion != 0 || guardar.getAsBoolean()) return;
        }
    }

    private void mostrarAdvertencias() {
        List<String> advertencias = datos.advertencias();
        if (advertencias.isEmpty()) {
            mostrarInformacion("Advertencias", "Los archivos de /data no tienen lineas invalidas ni referencias rotas.");
            return;
        }
        JTextArea lista = new JTextArea(String.join("\n", advertencias.stream().map(a -> "•  " + a).toList()));
        lista.setEditable(false);
        lista.setFont(Estilo.fuente(12, Font.PLAIN));
        lista.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setPreferredSize(new Dimension(560, 240));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(Estilo.parrafo("Estas lineas se ignoraron al calcular. El resto de los datos se procesa con normalidad.",
            540, Estilo.TEXTO_SUAVE), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, panel, "Advertencias", JOptionPane.WARNING_MESSAGE);
    }

    private void generarDatosDeEjemplo() {
        JSpinner alumnos = new JSpinner(new SpinnerNumberModel(ALUMNOS_POR_DEFECTO, 1, 500, 1));
        JSpinner cursos = new JSpinner(new SpinnerNumberModel(CURSOS_POR_DEFECTO, 1, 100, 1));
        Formulario.enfocarAlAbrir(alumnos);
        Formulario formulario = new Formulario()
            .campo("Alumnos", alumnos)
            .campo("Cursos", cursos)
            .nota("Cada alumno recibe entre 3 y 6 notas aleatorias en cursos existentes.", Estilo.TEXTO_SUAVE)
            .nota("Se reemplaza el contenido actual de alumnos.csv, cursos.csv y notas.txt.", Estilo.AVISO);

        formulario("Generar datos de ejemplo", formulario, "Generar", () -> intentar(() -> {
            int cantidadAlumnos = (Integer) alumnos.getValue();
            int cantidadCursos = (Integer) cursos.getValue();
            gestion.generarEjemplos(cantidadAlumnos, cantidadCursos);
            estado("Datos de ejemplo generados: " + Estilo.plural(cantidadAlumnos, "alumno", "alumnos")
                + " y " + Estilo.plural(cantidadCursos, "curso", "cursos") + ".");
            mostrar("Resultados");
        }));
    }
}
