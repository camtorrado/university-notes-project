package com.notas.gui;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.service.PromedioService;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Equivale a [8] Gestionar Notas de la consola. Se puede filtrar por alumno y por curso.
class PanelNotas extends Seccion {

    // Elemento de los desplegables: id null significa "todos".
    private record Opcion(String id, String texto) {
        @Override
        public String toString() {
            return texto;
        }
    }

    private final Tabla tabla;
    private final JComboBox<Opcion> filtroAlumno = new JComboBox<>();
    private final JComboBox<Opcion> filtroCurso = new JComboBox<>();
    private final BotonPlano eliminar;
    private final BotonPlano editar;
    private boolean reconstruyendoFiltros = false;
    private Datos datos = Datos.vacio();

    PanelNotas(VentanaPrincipal ventana) {
        super(ventana, "Notas", "Registro de notas.txt: una nota por alumno y curso, de 0.0 a 5.0. En rojo, las reprobadas (< "
            + Estilo.decimal(PromedioService.NOTA_APROBATORIA, 1) + ").");

        eliminar = accion(new BotonPlano("Eliminar", BotonPlano.Tipo.PELIGRO));
        editar = accion(new BotonPlano("Editar", BotonPlano.Tipo.SECUNDARIO));
        BotonPlano nueva = accion(new BotonPlano("Nueva nota", BotonPlano.Tipo.PRIMARIO));
        nueva.setToolTipText("Atajo: Ctrl/Cmd + N");

        tabla = new Tabla(
            new String[]{"ID alumno", "Alumno", "ID curso", "Curso", "Créditos", "Nota"},
            new Class<?>[]{String.class, String.class, String.class, String.class, Integer.class, Double.class},
            "No hay notas registradas.\nUsa «Nueva nota» o genera notas desde Alumnos o Cursos.",
            fila -> fila[0] + "|" + fila[2]);
        tabla.ordenarComoId(0, 2);
        tabla.ordenInicial(0, SortOrder.ASCENDING);
        tabla.renderizador(5, Estilo.renderNota());
        tabla.anchos(90, 230, 90, 230, 80, 70);

        filtroAlumno.addActionListener(e -> aplicarFiltro());
        filtroCurso.addActionListener(e -> aplicarFiltro());
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(etiquetaFiltro("Alumno"));
        filtros.add(filtroAlumno);
        filtros.add(etiquetaFiltro("  Curso"));
        filtros.add(filtroCurso);

        add(cuerpo(barra(filtros, conteo(tabla, "nota", "notas")), tabla));

        nueva.addActionListener(e -> nueva());
        editar.addActionListener(e -> editar());
        eliminar.addActionListener(e -> eliminar());
        tabla.alDobleClic(this::editar);
        tabla.alCambiarSeleccion(this::actualizarBotones);
        tabla.menuContextual(menu(
            "Editar", (Runnable) this::editar,
            null,
            "Eliminar", (Runnable) this::eliminar));

        atajoVentana(conModificador(KeyEvent.VK_N), this::nueva);
        tabla.atajo(ENTER, this::editar);
        tabla.atajo(SUPRIMIR, this::eliminar);
        tabla.atajo(RETROCESO, this::eliminar);
        actualizarBotones();
    }

    private static JLabel etiquetaFiltro(String texto) {
        return Estilo.texto(texto, 12, Font.PLAIN, Estilo.TEXTO_SUAVE);
    }

    @Override
    void actualizar(Datos datos) {
        this.datos = datos;
        reconstruirFiltros();

        List<Object[]> filas = new ArrayList<>();
        for (NotaCurso n : datos.notas()) {
            Alumno alumno = datos.alumnosPorId().get(n.alumnoId());
            Curso curso = datos.cursosPorId().get(n.cursoId());
            filas.add(new Object[]{
                n.alumnoId(), alumno == null ? "(alumno desconocido)" : alumno.nombreCompleto(),
                n.cursoId(), curso == null ? "(curso desconocido)" : curso.nombre(),
                curso == null ? null : curso.creditos(), n.nota()
            });
        }
        tabla.setFilas(filas);
        aplicarFiltro();
    }

    // Llamado desde Alumnos y Cursos con "Ver notas".
    void filtrar(String alumnoId, String cursoId) {
        seleccionarOpcion(filtroAlumno, alumnoId);
        seleccionarOpcion(filtroCurso, cursoId);
        aplicarFiltro();
    }

    // Rehace los desplegables con los datos nuevos, conservando lo que estaba elegido.
    private void reconstruirFiltros() {
        reconstruyendoFiltros = true;
        String alumnoElegido = idElegido(filtroAlumno);
        String cursoElegido = idElegido(filtroCurso);

        filtroAlumno.removeAllItems();
        filtroAlumno.addItem(new Opcion(null, "Todos"));
        datos.alumnos().forEach(a -> filtroAlumno.addItem(new Opcion(a.id(), a.id() + " · " + a.nombreCompleto())));
        filtroCurso.removeAllItems();
        filtroCurso.addItem(new Opcion(null, "Todos"));
        datos.cursos().forEach(c -> filtroCurso.addItem(new Opcion(c.id(), c.id() + " · " + c.nombre())));

        seleccionarOpcion(filtroAlumno, alumnoElegido);
        seleccionarOpcion(filtroCurso, cursoElegido);
        reconstruyendoFiltros = false;
    }

    private static String idElegido(JComboBox<Opcion> combo) {
        Opcion opcion = (Opcion) combo.getSelectedItem();
        return opcion == null ? null : opcion.id();
    }

    private static void seleccionarOpcion(JComboBox<Opcion> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (Objects.equals(combo.getItemAt(i).id(), id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        if (combo.getItemCount() > 0) combo.setSelectedIndex(0);
    }

    private void aplicarFiltro() {
        if (reconstruyendoFiltros) return;
        String alumnoId = idElegido(filtroAlumno);
        String cursoId = idElegido(filtroCurso);
        tabla.setFiltro(alumnoId == null && cursoId == null ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<?, ?> fila) {
                return (alumnoId == null || alumnoId.equals(fila.getStringValue(0)))
                    && (cursoId == null || cursoId.equals(fila.getStringValue(2)));
            }
        });
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean hay = tabla.seleccionada() != null;
        editar.setEnabled(hay);
        eliminar.setEnabled(hay);
    }

    private static JSpinner selectorNota(double inicial) {
        JSpinner nota = new JSpinner(new SpinnerNumberModel(inicial, 0.0, 5.0, 0.1));
        nota.setEditor(new JSpinner.NumberEditor(nota, "0.0"));
        return nota;
    }

    // El modelo del spinner suma 0.1 en coma flotante; se redondea a un decimal como en notas.txt.
    private static double valorNota(JSpinner nota) {
        return Math.round(((Number) nota.getValue()).doubleValue() * 10.0) / 10.0;
    }

    private void nueva() {
        if (datos.alumnos().isEmpty() || datos.cursos().isEmpty()) {
            ventana.mostrarError("Faltan datos", "Para registrar una nota primero debe existir al menos un alumno y un curso.");
            return;
        }
        JComboBox<Opcion> alumno = new JComboBox<>();
        datos.alumnos().forEach(a -> alumno.addItem(new Opcion(a.id(), a.id() + " · " + a.nombreCompleto())));
        JComboBox<Opcion> curso = new JComboBox<>();
        datos.cursos().forEach(c -> curso.addItem(new Opcion(c.id(), c.id() + " · " + c.nombre()
            + " (" + Estilo.plural(c.creditos(), "crédito", "créditos") + ")")));
        seleccionarOpcion(alumno, idElegido(filtroAlumno));
        seleccionarOpcion(curso, idElegido(filtroCurso));
        JSpinner nota = selectorNota(3.0);
        Formulario.enfocarAlAbrir(alumno);

        Formulario formulario = new Formulario()
            .campo("Alumno", alumno)
            .campo("Curso", curso)
            .campo("Nota", nota)
            .nota("Un alumno solo puede tener una nota por curso.", Estilo.TEXTO_SUAVE);

        ventana.formulario("Nueva nota", formulario, "Agregar", () -> ventana.intentar(() -> {
            String alumnoId = idElegido(alumno);
            String cursoId = idElegido(curso);
            ventana.gestion.agregarNota(alumnoId, cursoId, valorNota(nota));
            ventana.estado("Nota agregada para " + alumnoId + " en " + cursoId + ".");
            tabla.seleccionar(alumnoId + "|" + cursoId);
        }));
    }

    private void editar() {
        Object[] fila = tabla.seleccionada();
        if (fila == null) return;
        String alumnoId = (String) fila[0];
        String cursoId = (String) fila[2];
        JSpinner nota = selectorNota((Double) fila[5]);
        Formulario.enfocarAlAbrir(nota);

        Formulario formulario = new Formulario()
            .campo("Alumno", Estilo.texto(alumnoId + " · " + fila[1], 13, Font.PLAIN, Estilo.TEXTO))
            .campo("Curso", Estilo.texto(cursoId + " · " + fila[3], 13, Font.PLAIN, Estilo.TEXTO))
            .campo("Nota", nota);

        ventana.formulario("Editar nota", formulario, "Guardar", () -> ventana.intentar(() -> {
            ventana.gestion.editarNota(alumnoId, cursoId, valorNota(nota));
            ventana.estado("Nota de " + alumnoId + " en " + cursoId + " actualizada.");
        }));
    }

    private void eliminar() {
        Object[] fila = tabla.seleccionada();
        if (fila == null) return;
        String alumnoId = (String) fila[0];
        String cursoId = (String) fila[2];
        String mensaje = "Se eliminará la nota " + Estilo.decimal((Double) fila[5], 1) + " de " + fila[1]
            + " en " + fila[3] + ".";
        if (!ventana.confirmar("Eliminar nota", mensaje, "Eliminar")) return;

        ventana.intentar(() -> {
            ventana.gestion.eliminarNota(alumnoId, cursoId);
            ventana.estado("Nota de " + alumnoId + " en " + cursoId + " eliminada.");
        });
    }
}
