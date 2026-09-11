package com.notas.model;

// Mutable porque se va acumulando curso a curso durante el calculo del promedio ponderado.
public class PromedioAlumno {
    private final String id;
    private final String nombreCompleto;
    private double sumaPonderada;
    private int sumaCreditos;

    public PromedioAlumno(String id, String nombreCompleto) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
    }

    public void agregarNota(double nota, int creditos) {
        this.sumaPonderada += nota * creditos;
        this.sumaCreditos += creditos;
    }

    // Promedio ponderado = suma(nota * creditos) / suma(creditos). 0 si aun no tiene notas.
    public double getPromedio() {
        return sumaCreditos == 0 ? 0.0 : sumaPonderada / sumaCreditos;
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
}
