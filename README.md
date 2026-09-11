# Notas Universitarias

Calcula el promedio ponderado por créditos de cada estudiante a partir de sus notas
por curso, y genera un ranking (cuadro de honor) ordenado de mayor a menor promedio.

## Estructura (MVC)

```
src/com/notas/
├── model/         Alumno, NotaCurso, PromedioAlumno
├── repository/     Lectura de alumnos.csv y notas.txt
├── service/        PromedioService (cruce + promedio ponderado), GeneradorArchivosService (I/O dinamico)
├── view/           ConsoleView (menu y tablas)
├── controller/      NotasController (orquesta las acciones del menu)
└── App.java         Punto de entrada
data/                alumnos.csv, notas.txt, promedios.csv (se crean/actualizan en tiempo de ejecucion)
```

## Formatos de archivo

| Archivo            | Formato                             | Ejemplo                       |
|----------------------|-----------------------------------------|----------------------------------|
| alumnos.csv          | `ID;NombreCompleto`                   | `A1;Carlos Perez`               |
| notas.txt            | `ID;Curso;Nota;N_creditos`            | `A1;Calculo;4.5;3_creditos`     |
| promedios.csv (salida) | `ID;NombreCompleto;X.X_Prom`         | `A1;Carlos Perez;4.2_Prom`      |

Un mismo alumno puede tener varias líneas en `notas.txt` (una por curso cursado);
el promedio se calcula como `suma(nota x creditos) / suma(creditos)`.

## Generación de datos de ejemplo

La opción `[1]` genera `alumnos.csv` y `notas.txt` de forma **dinámica y
pseudoaleatoria**: combina nombres y apellidos de listas base, asigna entre 3 y 6
cursos por alumno con notas (2.0-5.0) y créditos (1-4) al azar en cada corrida,
manteniendo coherentes los IDs entre ambos archivos.

## Cuadro de honor

Un alumno se considera en cuadro de honor cuando su promedio ponderado es
`>= 4.5` (constante `PromedioService.UMBRAL_HONOR`). El ranking exportado ya
viene ordenado de mayor a menor promedio, así que los primeros de la lista son
los candidatos a beca; en consola además se marcan explícitamente con
`(Cuadro de Honor)`.

## Ejecutar en VSCode

1. Abrir la carpeta en VSCode con el Extension Pack for Java instalado.
2. Presionar F5, o desde terminal:
   ```
   javac -d bin $(find src -name "*.java")
   java -cp bin com.notas.App
   ```

## Menú

```
[1] Generar Archivos     -> crea alumnos.csv y notas.txt de ejemplo en /data
[2] Importar/Ver Base    -> lee y muestra alumnos y notas actuales
[3] Importar/Ver Salida  -> calcula el promedio ponderado y muestra el ranking
[4] Descargar Salida     -> exporta el ultimo ranking a data/promedios.csv
[5] Salir
```

## Manejo de casos límite

- Notas con un `ID` de alumno que no existe en el catálogo se ignoran silenciosamente
  (no rompen el cálculo).
- Líneas con formato inválido en `notas.txt` se descartan.
- La opción `[4]` exige haber corrido `[3]` primero en la misma sesión.
- Un alumno sin notas registradas tiene promedio 0.0 (no genera división por cero).
