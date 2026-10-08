package com.notas.gui;

import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.service.GestionService.ResultadoGeneracion;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

// Equivale a [7] Gestionar Cursos de la consola.
class PanelCursos extends Seccion {
    private final Tabla tabla;
    private final BotonMenu mas;
    private final BotonPlano eliminar;
    private final BotonPlano editar;
    private Datos datos = Datos.vacio();

    PanelCursos(VentanaPrincipal ventana) {
        super(ventana, "Cursos", "Catálogo de cursos.csv. Los créditos de cada curso ponderan sus notas en el promedio.");

        mas = accion(new BotonMenu("Más", menu(
            "Ver notas", (Runnable) this::verNotas,
            "Generar notas aleatorias", (Runnable) this::generarNotas)));
        eliminar = accion(new BotonPlano("Eliminar", BotonPlano.Tipo.PELIGRO));
        editar = accion(new BotonPlano("Editar", BotonPlano.Tipo.SECUNDARIO));
        BotonPlano nuevo = accion(new BotonPlano("Nuevo curso", BotonPlano.Tipo.PRIMARIO));
        nuevo.setToolTipText("Atajo: Ctrl/Cmd + N");

        tabla = new Tabla(
            new String[]{"ID", "Curso", "Créditos", "Horas/sem", "Inscritos", "Promedio del curso"},
            new Class<?>[]{String.class, String.class, Integer.class, Integer.class, Integer.class, Double.class},
            "No hay cursos registrados.\nUsa «Nuevo curso» o «Generar datos de ejemplo».",
            fila -> (String) fila[0]);
        tabla.ordenarComoId(0);
        tabla.ordenInicial(0, SortOrder.ASCENDING);
        tabla.decimales(5, 2);
        tabla.anchos(70, 260, 90, 100, 90, 140);

        CampoBusqueda busqueda = busqueda(tabla, "Buscar por ID o nombre", 0, 1);
        add(cuerpo(barra(busqueda, conteo(tabla, "curso", "cursos")), tabla));

        nuevo.addActionListener(e -> nuevo());
        editar.addActionListener(e -> editar());
        eliminar.addActionListener(e -> eliminar());
        tabla.alDobleClic(this::editar);
        tabla.alCambiarSeleccion(this::actualizarBotones);
        tabla.menuContextual(menu(
            "Editar", (Runnable) this::editar,
            "Ver notas", (Runnable) this::verNotas,
            "Generar notas aleatorias", (Runnable) this::generarNotas,
            null,
            "Eliminar", (Runnable) this::eliminar));

        atajoVentana(conModificador(KeyEvent.VK_N), this::nuevo);
        atajoVentana(conModificador(KeyEvent.VK_F), busqueda::requestFocusInWindow);
        tabla.atajo(ENTER, this::editar);
        tabla.atajo(SUPRIMIR, this::eliminar);
        tabla.atajo(RETROCESO, this::eliminar);
        actualizarBotones();
    }

    @Override
    void actualizar(Datos datos) {
        this.datos = datos;
        List<Object[]> filas = new ArrayList<>();
        for (Curso c : datos.cursos()) {
            List<NotaCurso> notas = datos.notasDeCurso(c.id());
            Double promedio = notas.isEmpty() ? null : notas.stream().mapToDouble(NotaCurso::nota).average().orElse(0);
            filas.add(new Object[]{c.id(), c.nombre(), c.creditos(), c.horasSemanales(), notas.size(), promedio});
        }
        tabla.setFilas(filas);
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean hay = tabla.seleccionada() != null;
        mas.setEnabled(hay);
        eliminar.setEnabled(hay);
        editar.setEnabled(hay);
    }

    private Curso seleccionado() {
        Object[] fila = tabla.seleccionada();
        return fila == null ? null : datos.cursosPorId().get((String) fila[0]);
    }

    private void verNotas() {
        Curso curso = seleccionado();
        if (curso != null) ventana.verNotas(null, curso.id());
    }

    private void nuevo() {
        JTextField nombre = new JTextField();
        JSpinner creditos = new JSpinner(new SpinnerNumberModel(3, 1, 30, 1));
        JSpinner horas = new JSpinner(new SpinnerNumberModel(4, 1, 60, 1));
        Formulario.enfocarAlAbrir(nombre);
        Formulario formulario = new Formulario()
            .campo("Nombre", nombre)
            .campo("Créditos", creditos)
            .campo("Horas semanales", horas)
            .nota("El ID se asigna automáticamente.", Estilo.TEXTO_SUAVE);

        ventana.formulario("Nuevo curso", formulario, "Agregar", () -> ventana.intentar(() -> {
            String id = ventana.gestion.agregarCurso(nombre.getText(), (Integer) creditos.getValue(), (Integer) horas.getValue());
            ventana.estado("Curso agregado con ID " + id + ".");
            tabla.seleccionar(id);
        }));
    }

    private void editar() {
        Curso curso = seleccionado();
        if (curso == null) return;
        JTextField nombre = new JTextField(curso.nombre());
        JSpinner creditos = new JSpinner(new SpinnerNumberModel(curso.creditos(), 1, Math.max(30, curso.creditos()), 1));
        JSpinner horas = new JSpinner(new SpinnerNumberModel(curso.horasSemanales(), 1, Math.max(60, curso.horasSemanales()), 1));
        Formulario.enfocarAlAbrir(nombre);
        Formulario formulario = new Formulario()
            .campo("Nombre", nombre)
            .campo("Créditos", creditos)
            .campo("Horas semanales", horas)
            .nota("Las notas registradas no cambian: el promedio y la carga horaria se recalculan con los nuevos valores.",
                Estilo.TEXTO_SUAVE);

        ventana.formulario("Editar curso " + curso.id(), formulario, "Guardar", () -> ventana.intentar(() -> {
            ventana.gestion.editarCurso(curso.id(), nombre.getText(), (Integer) creditos.getValue(), (Integer) horas.getValue());
            ventana.estado("Curso " + curso.id() + " actualizado.");
        }));
    }

    // Si el curso tiene notas, se ofrece borrarlo en cascada indicando cuantas se pierden.
    private void eliminar() {
        Curso curso = seleccionado();
        if (curso == null) return;
        int notas = datos.notasDeCurso(curso.id()).size();
        String mensaje = notas == 0
            ? "Se eliminará el curso " + curso.nombre() + " (" + curso.id() + ")."
            : curso.nombre() + " tiene " + Estilo.plural(notas, "nota registrada", "notas registradas")
                + ". Se eliminarán junto con el curso y los promedios de esos alumnos se recalcularán."
                + "\n\nEsta acción no se puede deshacer.";
        String textoAccion = notas == 0 ? "Eliminar" : "Eliminar curso y " + Estilo.plural(notas, "nota", "notas");
        if (!ventana.confirmar("Eliminar curso", mensaje, textoAccion)) return;

        ventana.intentar(() -> {
            int eliminadas = ventana.gestion.eliminarCurso(curso.id(), true);
            ventana.estado("Curso " + curso.id() + " eliminado"
                + (eliminadas > 0 ? " junto con " + Estilo.plural(eliminadas, "nota", "notas") : "") + ".");
        });
    }

    private void generarNotas() {
        Curso curso = seleccionado();
        if (curso == null) return;
        int alumnos = datos.alumnos().size();
        int notas = datos.notasDeCurso(curso.id()).size();
        String mensaje = "Se asignará una nota aleatoria en " + curso.nombre() + " a cada uno de los "
            + Estilo.plural(alumnos, "alumno", "alumnos") + " registrados."
            + (notas > 0 ? "\n\nReemplaza las " + Estilo.plural(notas, "nota actual", "notas actuales") + " del curso." : "");
        if (!ventana.confirmar("Generar notas del curso", mensaje, "Generar")) return;

        ventana.intentar(() -> {
            ResultadoGeneracion r = ventana.gestion.generarNotasCurso(curso.id());
            ventana.estado((r.reemplazo() ? "Notas recalculadas" : "Notas generadas") + " para el curso " + curso.id()
                + " (" + Estilo.plural(r.cantidad(), "alumno", "alumnos") + ").");
        });
    }
}
