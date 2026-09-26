# Notas Universitarias

Calcula el promedio ponderado por créditos de cada estudiante a partir de sus notas
por curso, y genera un ranking (cuadro de honor) ordenado de mayor a menor promedio.
Incluye gestión CRUD de alumnos, cursos y notas desde la consola.

## Estructura (MVC)

```
src/com/notas/
├── model/         Alumno, Curso, NotaCurso, PromedioAlumno, ResultadoCarga<T>
├── repository/     Lectura y escritura de alumnos.csv, cursos.csv y notas.txt
├── service/        PromedioService (cruce + promedio ponderado), GeneradorArchivosService (I/O dinamico)
├── view/           ConsoleView (menus y tablas)
├── controller/      NotasController (orquesta las acciones del menu y el CRUD)
└── App.java         Punto de entrada
data/                alumnos.csv, cursos.csv, notas.txt, promedios.csv (se crean/actualizan en tiempo de ejecucion)
```

`ResultadoCarga<T>` envuelve lo que devuelve cada lectura de archivo: la lista de
registros validos y, aparte, la lista de errores encontrados (linea y motivo). Asi
un archivo con datos incoherentes no detiene el programa, pero tampoco esconde el
problema.

## Formatos de archivo

| Archivo                | Formato                     | Ejemplo                  |
|--------------------------|----------------------------------|--------------------------------|
| alumnos.csv               | `ID;NombreCompleto`             | `A1;Carlos Perez`             |
| cursos.csv                 | `ID;Nombre;N_creditos;M_horas`  | `C1;Calculo;3_creditos;4_horas` |
| notas.txt                  | `AlumnoId;CursoId;Nota`         | `A1;C1;4.5`                   |
| promedios.csv (salida) | `N_Puesto;ID;NombreCompleto;X.X_Prom;SI\|NO_Beca;SI\|NO_Honor;M_HorasSemanales` | `1_Puesto;A1;Carlos Perez;4.6_Prom;SI_Beca;SI_Honor;5_HorasSemanales` |

Un mismo alumno puede tener varias líneas en `notas.txt` (una por curso cursado).
Los créditos **solo** viven en `cursos.csv`; el promedio se calcula como
`suma(nota x creditos_del_curso) / suma(creditos_del_curso)`. Esto evita que la
misma información quede duplicada en dos archivos y se desincronice.

## Generación de datos de ejemplo

La opción `[1]` pregunta cuántos alumnos y cuántos cursos generar (Enter usa 8 y 6
por defecto) y crea `cursos.csv`, `alumnos.csv` y `notas.txt` de forma dinámica y
pseudoaleatoria: cada nota generada referencia un alumno y un curso que realmente
existen en los catálogos recién escritos.

## Cuadro de honor y beca

Un alumno se considera en cuadro de honor cuando su promedio ponderado es
`>= 4.5` (constante `PromedioService.UMBRAL_HONOR`); en consola se marca
explícitamente con `(Cuadro de Honor)`.

Aplica para beca cuando su promedio ponderado es `>= 4.0` (constante
`PromedioService.UMBRAL_BECA`). Esto se ve como columna `Beca` en consola
(`[3]`) y como `SI_Beca`/`NO_Beca` en `promedios.csv` al exportar (`[4]`).

El ranking exportado en `promedios.csv` incluye ademas: el puesto (`N_Puesto`,
segun el orden de mayor a menor promedio), si esta en cuadro de honor
(`SI_Honor`/`NO_Honor`) y la carga horaria semanal del alumno
(`M_HorasSemanales`). El puesto tambien se muestra en consola en `[3]`.

## Carga horaria semanal

Cada curso tiene unas horas semanales (independientes de los créditos), que
se guardan en `cursos.csv`. La carga horaria de un alumno es la suma de las
horas de todos los cursos en los que tiene nota (esta matriculado); se
muestra junto a su nombre en `[2] Importar/Ver Base` y en `[6] Gestionar
Alumnos`. Como no se guarda por separado, siempre queda al día: editar las
horas de un curso actualiza automáticamente la carga de sus alumnos.

## Ejecutar en VSCode

1. Abrir la carpeta en VSCode con el Extension Pack for Java instalado.
2. Presionar F5, o desde terminal:
   ```
   javac -d bin $(find src -name "*.java")
   java -cp bin com.notas.App
   ```

## Menú

```
[1] Generar Archivos     -> crea cursos.csv, alumnos.csv y notas.txt de ejemplo en /data
[2] Importar/Ver Base    -> lee y muestra alumnos, cursos y notas actuales, con advertencias de datos invalidos
[3] Importar/Ver Salida  -> calcula el promedio ponderado y muestra el ranking
[4] Descargar Salida     -> exporta el ultimo ranking a data/promedios.csv
[5] Limpiar Salida       -> borra promedios.csv (el ranking se recalcula siempre desde los archivos actuales, no depende de esto)
[6] Gestionar Alumnos    -> CRUD (agregar, editar, eliminar) + generar notas aleatorias para un alumno
[7] Gestionar Cursos     -> CRUD (agregar, editar, eliminar) + generar notas aleatorias para todos los alumnos en un curso
[8] Gestionar Notas      -> CRUD: agregar, editar, eliminar
[9] Acerca de            -> integrantes del grupo y proposito del programa
[10] Salir
```

## CRUD y consistencia de datos

- **Los IDs de alumno y curso se asignan automaticamente** al agregar uno
  nuevo (siguiente numero disponible con el prefijo `A` o `C`); no se piden
  por consola, para evitar colisiones y errores de digitacion.
- **Generar notas aleatorias** es una feature aparte del CRUD normal, para
  completar datos de prueba sin cargarlos a mano, en dos direcciones
  simetricas:
  - Desde `Gestionar Alumnos`: para un alumno puntual, genera entre 3 y 6
    notas pseudoaleatorias repartidas en los cursos existentes.
  - Desde `Gestionar Cursos`: para un curso puntual, genera una nota
    pseudoaleatoria para cada alumno del catalogo (todos quedan con nota en
    ese curso).
  En ambos casos, si ya existian notas en ese alcance se reemplazan
  (recalculo); si no existian, se crean. Nunca se disparan solas, siempre son
  una accion explicita.
- **Eliminar un alumno** borra en cascada todas sus notas, para no dejar
  referencias huérfanas en `notas.txt`.
- **No se permiten nombres duplicados**: al agregar o editar un alumno o un
  curso, se rechaza si ya existe otro con el mismo nombre, sin importar
  mayusculas o minusculas ("Redes" y "redes" cuentan como el mismo nombre).
- **Eliminar un curso** se bloquea si hay notas que lo referencian; el mensaje
  indica cuántas, para que primero se eliminen o reasignen.
- **Editar un curso** (por ejemplo sus créditos) no reescribe las notas ya
  registradas: el promedio se recalcula siempre contra el valor actual del
  catálogo.
- `[3] Importar/Ver Salida` y `[4] Descargar Salida` no dependen de ningún
  cálculo anterior guardado en memoria: cada una relee `alumnos.csv`,
  `cursos.csv` y `notas.txt` y recalcula el ranking completo en el momento.
  Por eso un cambio hecho en el CRUD (alta, edición o baja, en cualquiera de
  los tres catálogos) queda reflejado automáticamente la próxima vez que se
  use `[4]`, sin necesidad de pasar antes por `[3]`.

## Manejo de casos límite

- **Un alumno no puede tener dos notas en el mismo curso** (relación única
  alumno-curso). Se valida al agregar una nota a mano, y la generación
  aleatoria de notas (tanto la inicial de `[1]` como la de "Generar notas
  aleatorias" en Alumnos/Cursos) nunca repite el par alumno-curso.
- Una línea con formato inválido, un ID duplicado, una nota fuera de 0.0-5.0 o
  unos créditos no positivos se reportan como advertencia y se descartan; el
  resto del archivo se sigue procesando con normalidad.
- Una nota que referencia un alumno o un curso que no existe en el catálogo
  también se reporta como advertencia (no rompe el cálculo del promedio).
- Un alumno sin notas registradas tiene promedio 0.0 (no genera división por cero).
