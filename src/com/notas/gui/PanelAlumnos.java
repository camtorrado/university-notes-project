package com.notas.gui;

import com.notas.model.Alumno;
import com.notas.model.PromedioAlumno;
import com.notas.service.GestionService.ResultadoGeneracion;

import javax.swing.JTextField;
import javax.swing.SortOrder;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

// Equivale a [6] Gestionar Alumnos de la consola.
class PanelAlumnos extends Seccion {
    private final Tabla tabla;
    private final BotonMenu mas;
    private final BotonPlano eliminar;
    private final BotonPlano editar;
    private Datos datos = Datos.vacio();

    PanelAlumnos(VentanaPrincipal ventana) {
        super(ventana, "Alumnos", "Catálogo de alumnos.csv. Las horas suman los cursos en los que el alumno tiene nota.");

        mas = accion(new BotonMenu("Más", menu(
            "Ver ficha", (Runnable) this::verFicha,
            "Ver notas", (Runnable) this::verNotas,
            "Generar notas aleatorias", (Runnable) this::generarNotas)));
        eliminar = accion(new BotonPlano("Eliminar", BotonPlano.Tipo.PELIGRO));
        editar = accion(new BotonPlano("Editar", BotonPlano.Tipo.SECUNDARIO));
        BotonPlano nuevo = accion(new BotonPlano("Nuevo alumno", BotonPlano.Tipo.PRIMARIO));
        nuevo.setToolTipText("Atajo: Ctrl/Cmd + N");

        tabla = new Tabla(
            new String[]{"ID", "Nombre", "Cursos", "Reprobadas", "Horas/sem", "Promedio"},
            new Class<?>[]{String.class, String.class, Integer.class, Integer.class, Integer.class, Double.class},
            "No hay alumnos registrados.\nUsa «Nuevo alumno» o «Generar datos de ejemplo».",
            fila -> (String) fila[0]);
        tabla.ordenarComoId(0);
        tabla.ordenInicial(0, SortOrder.ASCENDING);
        tabla.decimales(5, 2);
        tabla.renderizador(3, new Estilo.RenderCelda(0, v -> v instanceof Integer n && n > 0 ? Estilo.PELIGRO : null));
        tabla.anchos(70, 280, 80, 100, 100, 100);

        CampoBusqueda busqueda = busqueda(tabla, "Buscar por ID o nombre", 0, 1);
        add(cuerpo(barra(busqueda, conteo(tabla, "alumno", "alumnos")), tabla));

        nuevo.addActionListener(e -> nuevo());
        editar.addActionListener(e -> editar());
        eliminar.addActionListener(e -> eliminar());
        tabla.alDobleClic(this::verFicha);
        tabla.alCambiarSeleccion(this::actualizarBotones);
        tabla.menuContextual(menu(
            "Ver ficha", (Runnable) this::verFicha,
            "Editar", (Runnable) this::editar,
            "Ver notas", (Runnable) this::verNotas,
            "Generar notas aleatorias", (Runnable) this::generarNotas,
            null,
            "Eliminar", (Runnable) this::eliminar));

        atajoVentana(conModificador(KeyEvent.VK_N), this::nuevo);
        atajoVentana(conModificador(KeyEvent.VK_F), busqueda::requestFocusInWindow);
        tabla.atajo(ENTER, this::verFicha);
        tabla.atajo(SUPRIMIR, this::eliminar);
        tabla.atajo(RETROCESO, this::eliminar);
        actualizarBotones();
    }

    @Override
    void actualizar(Datos datos) {
        this.datos = datos;
        List<Object[]> filas = new ArrayList<>();
        for (Alumno a : datos.alumnos()) {
            PromedioAlumno promedio = datos.promedioPorAlumno().get(a.id());
            filas.add(new Object[]{
                a.id(), a.nombreCompleto(), datos.notasDeAlumno(a.id()).size(),
                datos.reprobadosPorAlumno().getOrDefault(a.id(), 0),
                datos.horasPorAlumno().getOrDefault(a.id(), 0),
                promedio == null ? null : promedio.getPromedio()
            });
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

    private Alumno seleccionado() {
        Object[] fila = tabla.seleccionada();
        return fila == null ? null : datos.alumnosPorId().get((String) fila[0]);
    }

    private void verFicha() {
        Alumno alumno = seleccionado();
        if (alumno == null) return;
        ventana.intentar(() -> new FichaAlumno(ventana, ventana.gestion.fichaAlumno(alumno.id()), this::editar).setVisible(true));
    }

    private void verNotas() {
        Alumno alumno = seleccionado();
        if (alumno != null) ventana.verNotas(alumno.id(), null);
    }

    private void nuevo() {
        JTextField nombre = new JTextField();
        Formulario.enfocarAlAbrir(nombre);
        Formulario formulario = new Formulario()
            .campo("Nombre completo", nombre)
            .nota("El ID se asigna automáticamente.", Estilo.TEXTO_SUAVE);

        ventana.formulario("Nuevo alumno", formulario, "Agregar", () -> ventana.intentar(() -> {
            String id = ventana.gestion.agregarAlumno(nombre.getText());
            ventana.estado("Alumno agregado con ID " + id + ".");
            tabla.seleccionar(id);
        }));
    }

    private void editar() {
        Alumno alumno = seleccionado();
        if (alumno == null) return;
        JTextField nombre = new JTextField(alumno.nombreCompleto());
        Formulario.enfocarAlAbrir(nombre);
        Formulario formulario = new Formulario().campo("Nombre completo", nombre);

        ventana.formulario("Editar alumno " + alumno.id(), formulario, "Guardar", () -> ventana.intentar(() -> {
            ventana.gestion.editarAlumno(alumno.id(), nombre.getText());
            ventana.estado("Alumno " + alumno.id() + " actualizado.");
        }));
    }

    private void eliminar() {
        Alumno alumno = seleccionado();
        if (alumno == null) return;
        int notas = datos.notasDeAlumno(alumno.id()).size();
        String mensaje = "Se eliminará a " + alumno.nombreCompleto() + " (" + alumno.id() + ")"
            + (notas > 0 ? " junto con sus " + Estilo.plural(notas, "nota", "notas") + "." : ".")
            + "\n\nEsta acción no se puede deshacer.";
        if (!ventana.confirmar("Eliminar alumno", mensaje, "Eliminar")) return;

        ventana.intentar(() -> {
            int eliminadas = ventana.gestion.eliminarAlumno(alumno.id());
            ventana.estado("Alumno " + alumno.id() + " eliminado (" + Estilo.plural(eliminadas, "nota asociada", "notas asociadas") + ").");
        });
    }

    private void generarNotas() {
        Alumno alumno = seleccionado();
        if (alumno == null) return;
        int notas = datos.notasDeAlumno(alumno.id()).size();
        String mensaje = "Se asignarán entre 3 y 6 notas aleatorias a " + alumno.nombreCompleto()
            + " en cursos existentes."
            + (notas > 0 ? "\n\nReemplaza sus " + Estilo.plural(notas, "nota actual", "notas actuales") + "." : "");
        if (!ventana.confirmar("Generar notas", mensaje, "Generar")) return;

        ventana.intentar(() -> {
            ResultadoGeneracion r = ventana.gestion.generarNotasAlumno(alumno.id());
            ventana.estado((r.reemplazo() ? "Notas recalculadas" : "Notas generadas") + " para " + alumno.id()
                + " (" + Estilo.plural(r.cantidad(), "nota", "notas") + ").");
        });
    }
}
