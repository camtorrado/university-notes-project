package com.notas.model;

// Mutable porque se va acumulando curso a curso durante el calculo del promedio ponderado.
public class PromedioAlumno {
    private final String id;
    private final String nombreCompleto;
    private double sumaPonderada;
    private int sumaCreditos;
    private int puesto;

    public PromedioAlumno(String id, String nombreCompleto) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
    }

    public void agregarNota(double nota, int creditos) {
        this.sumaPonderada += nota * creditos;
        this.sumaCreditos += creditos;
    }

    // Promedio oficial = suma(nota * creditos) / suma(creditos), redondeado a 2 decimales.
    // Es el valor que se muestra, se exporta y con el que se deciden beca, honor y empates,
    // para que nunca se contradigan. 0 si aun no tiene notas.
    public double getPromedio() {
        if (sumaCreditos == 0) return 0.0;
        return Math.round(sumaPonderada / sumaCreditos * 100.0) / 100.0;
    }

    public String getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public int getSumaCreditos() {
        return sumaCreditos;
    }

    // Lo asigna PromedioService al ordenar el ranking; empates comparten puesto.
    public int getPuesto() {
        return puesto;
    }

    public void setPuesto(int puesto) {
        this.puesto = puesto;
    }
}
