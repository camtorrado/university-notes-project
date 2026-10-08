# Notas Universitarias

Calcula el promedio ponderado por créditos de cada estudiante a partir de sus notas
por curso, y genera un ranking (cuadro de honor) ordenado de mayor a menor promedio.
Incluye gestión CRUD de alumnos, cursos y notas desde la consola.

## Estructura (MVC)

```
src/com/notas/
├── model/         Alumno, Curso, NotaCurso, PromedioAlumno, ResultadoCarga<T>
├── repository/     Lectura y escritura de alumnos.csv, cursos.csv, notas.txt y promedios.csv
├── service/        GestionService (reglas del CRUD, ficha y salida), PromedioService (promedio, ranking,
│                   umbrales), GeneradorDatosService (datos de ejemplo, sin I/O)
├── view/           ConsoleView (menus y tablas)
├── controller/      NotasController (menu de consola, delega las reglas en GestionService)
├── gui/             Interfaz grafica Swing (VentanaPrincipal + una seccion por pantalla)
├── App.java         Punto de entrada de consola
├── AppGUI.java      Punto de entrada de la interfaz grafica
└── Proyecto.java    Nombre, asignatura e integrantes (compartido por consola y GUI)
test/com/notas/
└── Pruebas.java     Pruebas de la logica en Java puro (sin librerias)
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
| promedios.csv (salida) | `N_Puesto;ID;NombreCompleto;X.XX_Prom;SI\|NO_Beca;SI\|NO_Honor;M_HorasSemanales` | `1_Puesto;A1;Carlos Perez;4.62_Prom;SI_Beca;SI_Honor;5_HorasSemanales` |

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

El promedio oficial se redondea a **2 decimales**, y con ese mismo valor se decide la
beca, el cuadro de honor y los empates: lo que se ve en pantalla y en `promedios.csv`
nunca se contradice (un 3.995 se muestra como 4.00 y si aplica a beca).

**Empates:** alumnos con el mismo promedio comparten puesto y el siguiente salta
(1, 2, 2, 4). Dentro del empate se listan primero quienes cursaron mas creditos y
luego por nombre. Los alumnos sin ninguna nota no entran al ranking; la consola y la
GUI indican cuantos son.

**Nota aprobatoria:** 3.0 (`PromedioService.NOTA_APROBATORIA`). Es solo informativa:
las notas reprobadas se marcan (en rojo en la GUI, con `(reprobada)` en consola) y
cada alumno muestra cuantas tiene, pero no cambia la beca ni el cuadro de honor.

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
2. Elegir "Launch GUI" (interfaz grafica) o "Launch App" (consola) y presionar F5, o desde terminal:
   ```
   javac -d bin $(find src -name "*.java")
   java -cp bin com.notas.AppGUI   # interfaz grafica
   java -cp bin com.notas.App      # consola
   ```
   Ambas versiones leen y escriben los mismos archivos de `data/` y aplican las mismas
   reglas (viven en `GestionService`), asi que se pueden usar indistintamente.

## Interfaz grafica

Barra lateral con las secciones, contenido al centro y barra de estado abajo
(mensaje de la ultima accion, advertencias de los archivos y "Recargar archivos").

| Seccion    | Equivale en consola | Que muestra / permite |
|------------|---------------------|------------------------|
| Resultados | [3], [4], [5]       | Indicadores (evaluados, promedio del grupo, becas, cuadro de honor), grafico de distribucion de promedios, ranking con busqueda y filtro (beca / cuadro de honor), Exportar CSV, Limpiar salida y "Mostrar en carpeta". Avisa si `promedios.csv` quedo desactualizado frente a los datos. |
| Alumnos    | [6]                 | Tabla con cursos, reprobadas, horas semanales y promedio; nuevo, editar, eliminar. En "Más ▾": ver ficha, ver notas y generar notas. Doble clic abre la ficha. |
| Cursos     | [7]                 | Tabla con creditos, horas, inscritos y promedio del curso; nuevo, editar, eliminar. En "Más ▾": ver notas y generar notas. |
| Notas      | [8]                 | Tabla filtrable por alumno y por curso, con las reprobadas en rojo; nueva, editar y eliminar. |
| Generar datos de ejemplo | [1]   | Dialogo con la cantidad de alumnos y cursos. |
| Acerca de  | [9]                 | Proposito, reglas de calculo, archivos e integrantes. |

`[2] Importar/Ver Base` corresponde a las tablas de Alumnos, Cursos y Notas mas el
contador de advertencias de la barra de estado. Las tablas se ordenan haciendo clic en
el encabezado; clic derecho sobre una fila muestra todas sus acciones.

Atajos: `Ctrl/Cmd + N` nuevo, `Ctrl/Cmd + F` buscar, `Enter` abrir (ficha o edicion),
`Supr` eliminar la fila seleccionada. Las acciones destructivas
piden confirmacion y explican la consecuencia (por ejemplo, cuantas notas se borran).

## Menú

```
[1] Generar Archivos     -> crea cursos.csv, alumnos.csv y notas.txt de ejemplo en /data
[2] Importar/Ver Base    -> lee y muestra alumnos, cursos y notas actuales, con advertencias de datos invalidos
[3] Importar/Ver Salida  -> calcula el promedio ponderado y muestra el ranking
[4] Descargar Salida     -> exporta el ultimo ranking a data/promedios.csv
[5] Limpiar Salida       -> borra promedios.csv (el ranking se recalcula siempre desde los archivos actuales, no depende de esto)
[6] Gestionar Alumnos    -> CRUD (agregar, editar, eliminar) + generar notas aleatorias + ver ficha del alumno
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
  mayusculas, minusculas ni espacios repetidos ("Redes" y " redes " cuentan como
  el mismo nombre). Los nombres no pueden contener `;` (es el separador de los archivos).
- **Ficha del alumno**: promedio, puesto, creditos, horas, notas reprobadas,
  cuanto le falta para beca y cuadro de honor, y el aporte de cada curso
  (nota x creditos) al promedio.
- **Eliminar un curso** que tiene notas pide confirmacion explicita indicando
  cuantas notas se borran con el (en consola: `Eliminar tambien sus notas? (s/n)`;
  si se responde que no, el curso no se elimina).
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
- Si `notas.txt` trae dos notas del mismo alumno en el mismo curso (editado a mano),
  solo cuenta la primera y la segunda se reporta como advertencia.
- Los decimales se escriben siempre con punto, aunque el sistema este configurado en
  español (que usaria coma y romperia la lectura).

## Pruebas

`test/com/notas/Pruebas.java` cubre el calculo (promedio, redondeo, umbrales, empates,
carga horaria), la lectura de archivos (lineas invalidas, duplicados, idioma es-CO),
las reglas del CRUD, la salida y el generador. Cada prueba usa una carpeta temporal,
nunca `data/`. En VSCode: "Run Pruebas"; desde terminal:

```
javac -d bin $(find src test -name "*.java")
java -cp bin com.notas.Pruebas
```
