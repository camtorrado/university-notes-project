package com.notas;

import java.util.List;

// Datos del proyecto que muestran tanto la consola como la interfaz grafica.
public final class Proyecto {
    public static final String NOMBRE = "Notas Universitarias";
    public static final String INSTITUCION = "Politecnico Grancolombiano";
    public static final String ASIGNATURA = "Conceptos Fundamentales de Programacion";
    public static final List<String> INTEGRANTES = List.of(
        "Deyner Camilo Torrado",
        "Ana Maria Bedoya",
        "Kevin Andres Villamizar",
        "Catalina Hernandez Saldarriaga",
        "Yenith Guasca Susa"
    );

    private Proyecto() {
    }
}
