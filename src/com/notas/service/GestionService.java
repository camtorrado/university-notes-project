package com.notas.service;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.model.ResultadoCarga;
import com.notas.repository.AlumnoRepository;
import com.notas.repository.CursoRepository;
import com.notas.repository.NotaRepository;
import com.notas.repository.SalidaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Reglas de negocio del CRUD y de la salida, compartidas por la consola y la interfaz grafica.
// Cada operacion relee los archivos, valida y guarda: nunca se trabaja sobre una copia vieja.
// Si una regla no se cumple se lanza OperacionInvalidaException con un mensaje para el usuario.
public class GestionService {

    // Foto de los tres archivos de entrada, con las advertencias encontradas al leerlos.
    public record Base(List<Alumno> alumnos, List<Curso> cursos, List<NotaCurso> notas, List<String> advertencias) {
    }

    // Cuantas notas se generaron y si reemplazaron notas que ya existian.
    public record ResultadoGeneracion(int cantidad, boolean reemplazo) {
    }

    // DESACTUALIZADA: algun archivo de entrada cambio despues de la ultima exportacion.
    public enum EstadoSalida { NO_GENERADA, AL_DIA, DESACTUALIZADA }

    // Una linea de la ficha: la nota en un curso y cuanto aporta al promedio (nota x creditos).
    public record NotaFicha(String cursoId, String curso, int creditos, double nota, double aporte, boolean aprobada) {
    }

    // Resumen de un alumno. puesto es null si aun no tiene notas (no aparece en el ranking).
    public record Ficha(Alumno alumno, double promedio, Integer puesto, int creditos, int horas, int reprobados,
                        boolean beca, boolean honor, double faltaBeca, double faltaHonor, List<NotaFicha> notas) {
    }

    private final Path dirDatos;
    private final Path alumnosPath;
    private final Path cursosPath;
    private final Path notasPath;
    private final AlumnoRepository alumnoRepo;
    private final CursoRepository cursoRepo;
    private final NotaRepository notaRepo;
    private final SalidaRepository salidaRepo;
    private final PromedioService promedioService = new PromedioService();
    private final GeneradorDatosService generador = new GeneradorDatosService();

    public GestionService(Path dirDatos) {
        this.dirDatos = dirDatos;
        this.alumnosPath = dirDatos.resolve("alumnos.csv");
        this.cursosPath = dirDatos.resolve("cursos.csv");
        this.notasPath = dirDatos.resolve("notas.txt");
        this.alumnoRepo = new AlumnoRepository(alumnosPath);
        this.cursoRepo = new CursoRepository(cursosPath);
        this.notaRepo = new NotaRepository(notasPath);
        this.salidaRepo = new SalidaRepository(dirDatos.resolve("promedios.csv"));
    }

    // ---------- Lectura ----------

    public Base cargarBase() throws IOException {
        ResultadoCarga<Alumno> alumnos = alumnoRepo.cargar();
        ResultadoCarga<Curso> cursos = cursoRepo.cargar();
        ResultadoCarga<NotaCurso> notas = notaRepo.cargar();

        List<String> advertencias = new ArrayList<>();
        advertencias.addAll(alumnos.errores());
        advertencias.addAll(cursos.errores());
        advertencias.addAll(notas.errores());
        return new Base(alumnos.registros(), cursos.registros(), notas.registros(), advertencias);
    }

    // Recalcula desde los archivos actuales, nunca queda desactualizado frente al CRUD.
    public ResultadoCarga<PromedioAlumno> calcularRanking() throws IOException {
        return calcularRanking(cargarBase());
    }

    public ResultadoCarga<PromedioAlumno> calcularRanking(Base base) {
        ResultadoCarga<PromedioAlumno> calculo = promedioService.calcularRanking(
            mapPorId(base.alumnos(), Alumno::id), mapPorId(base.cursos(), Curso::id), base.notas());

        List<String> advertencias = new ArrayList<>(base.advertencias());
        advertencias.addAll(calculo.errores());
        return new ResultadoCarga<>(calculo.registros(), advertencias);
    }

    // Suma las horas semanales de cada curso en el que el alumno tiene nota (esta matriculado).
    public static Map<String, Integer> calcularHorasPorAlumno(Map<String, Curso> cursosPorId, List<NotaCurso> notas) {
        Map<String, Integer> horasPorAlumno = new LinkedHashMap<>();
        for (NotaCurso nota : notas) {
            Curso curso = cursosPorId.get(nota.cursoId());
            if (curso == null) continue;
            horasPorAlumno.merge(nota.alumnoId(), curso.horasSemanales(), Integer::sum);
        }
        return horasPorAlumno;
    }

    // Cuantas notas por debajo de la nota aprobatoria tiene cada alumno.
    public static Map<String, Integer> contarReprobadosPorAlumno(List<NotaCurso> notas) {
        Map<String, Integer> reprobados = new LinkedHashMap<>();
        for (NotaCurso nota : notas) {
            if (!PromedioService.estaAprobada(nota.nota())) reprobados.merge(nota.alumnoId(), 1, Integer::sum);
        }
        return reprobados;
    }

    // Alumnos del catalogo que no aparecen en el ranking por no tener ninguna nota valida.
    public static long contarSinNotas(Base base, List<PromedioAlumno> ranking) {
        return base.alumnos().size() - ranking.size();
    }

    public static <T> Map<String, T> mapPorId(List<T> lista, Function<T, String> idExtractor) {
        return lista.stream().collect(Collectors.toMap(idExtractor, x -> x, (a, b) -> a, LinkedHashMap::new));
    }

    // ---------- Archivos ----------

    public void generarEjemplos(int cantidadAlumnos, int cantidadCursos) throws IOException {
        Files.createDirectories(dirDatos);
        GeneradorDatosService.DatosGenerados datos = generador.generarEjemplos(cantidadAlumnos, cantidadCursos);
        cursoRepo.guardarTodos(datos.cursos());
        alumnoRepo.guardarTodos(datos.alumnos());
        notaRepo.guardarTodos(datos.notas());
    }

    // Devuelve la ruta del archivo exportado.
    public Path exportarRanking() throws IOException {
        Base base = cargarBase();
        ResultadoCarga<PromedioAlumno> ranking = calcularRanking(base);
        if (ranking.registros().isEmpty()) {
            throw new OperacionInvalidaException("No hay datos para exportar: revise que haya alumnos, cursos y notas cargados.");
        }
        Map<String, Integer> horas = calcularHorasPorAlumno(mapPorId(base.cursos(), Curso::id), base.notas());
        salidaRepo.guardar(ranking.registros(), horas);
        return salidaRepo.getRuta();
    }

    // true si habia una salida y se elimino.
    public boolean limpiarSalida() throws IOException {
        return salidaRepo.eliminar();
    }

    public EstadoSalida estadoSalida() throws IOException {
        FileTime exportada = salidaRepo.ultimaExportacion();
        if (exportada == null) return EstadoSalida.NO_GENERADA;
        for (Path entrada : List.of(alumnosPath, cursosPath, notasPath)) {
            if (Files.exists(entrada) && Files.getLastModifiedTime(entrada).compareTo(exportada) > 0) {
                return EstadoSalida.DESACTUALIZADA;
            }
        }
        return EstadoSalida.AL_DIA;
    }

    public Path getDirDatos() {
        return dirDatos;
    }

    // ---------- Alumnos ----------

    // Devuelve el ID asignado.
    public String agregarAlumno(String nombre) throws IOException {
        List<Alumno> alumnos = new ArrayList<>(alumnoRepo.cargar().registros());
        String nombreLimpio = validarNombre(nombre);
        if (nombreDuplicado(alumnos.stream().map(Alumno::nombreCompleto).toList(), nombreLimpio)) {
            throw new OperacionInvalidaException("Ya existe un alumno con ese nombre.");
        }
        String id = generarSiguienteId(alumnos.stream().map(Alumno::id).toList(), "A");
        alumnos.add(new Alumno(id, nombreLimpio));
        alumnoRepo.guardarTodos(alumnos);
        return id;
    }

    public void editarAlumno(String id, String nuevoNombre) throws IOException {
        List<Alumno> alumnos = new ArrayList<>(alumnoRepo.cargar().registros());
        int indice = indiceDe(alumnos, a -> a.id().equals(id));
        if (indice < 0) throw new OperacionInvalidaException("No existe un alumno con ese ID.");

        String nombreLimpio = validarNombre(nuevoNombre);
        List<String> otrosNombres = alumnos.stream()
            .filter(a -> !a.id().equals(id)).map(Alumno::nombreCompleto).toList();
        if (nombreDuplicado(otrosNombres, nombreLimpio)) {
            throw new OperacionInvalidaException("Ya existe un alumno con ese nombre.");
        }
        alumnos.set(indice, new Alumno(id, nombreLimpio));
        alumnoRepo.guardarTodos(alumnos);
    }

    // Borra tambien las notas del alumno para no dejar referencias huerfanas. Devuelve cuantas.
    public int eliminarAlumno(String id) throws IOException {
        List<Alumno> alumnos = new ArrayList<>(alumnoRepo.cargar().registros());
        if (!alumnos.removeIf(a -> a.id().equals(id))) {
            throw new OperacionInvalidaException("No existe un alumno con ese ID.");
        }
        alumnoRepo.guardarTodos(alumnos);

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        int eliminadas = (int) notas.stream().filter(n -> n.alumnoId().equals(id)).count();
        notas.removeIf(n -> n.alumnoId().equals(id));
        notaRepo.guardarTodos(notas);
        return eliminadas;
    }

    // Feature aparte del CRUD: notas pseudoaleatorias para un alumno (recalcula si ya tenia).
    public ResultadoGeneracion generarNotasAlumno(String id) throws IOException {
        if (alumnoRepo.cargar().registros().stream().noneMatch(a -> a.id().equals(id))) {
            throw new OperacionInvalidaException("No existe un alumno con ese ID.");
        }
        List<Curso> cursos = cursoRepo.cargar().registros();
        if (cursos.isEmpty()) {
            throw new OperacionInvalidaException("No hay cursos registrados. Cree al menos un curso primero.");
        }

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        boolean teniaNotas = notas.removeIf(n -> n.alumnoId().equals(id));
        List<NotaCurso> notasNuevas = generador.generarNotasAleatorias(id, cursos);
        notas.addAll(notasNuevas);
        notaRepo.guardarTodos(notas);
        return new ResultadoGeneracion(notasNuevas.size(), teniaNotas);
    }

    // ---------- Cursos ----------

    // Devuelve el ID asignado.
    public String agregarCurso(String nombre, int creditos, int horas) throws IOException {
        List<Curso> cursos = new ArrayList<>(cursoRepo.cargar().registros());
        String nombreLimpio = validarNombre(nombre);
        if (nombreDuplicado(cursos.stream().map(Curso::nombre).toList(), nombreLimpio)) {
            throw new OperacionInvalidaException("Ya existe un curso con ese nombre.");
        }
        validarCreditosYHoras(creditos, horas);

        String id = generarSiguienteId(cursos.stream().map(Curso::id).toList(), "C");
        cursos.add(new Curso(id, nombreLimpio, creditos, horas));
        cursoRepo.guardarTodos(cursos);
        return id;
    }

    // No reescribe las notas: el promedio se recalcula siempre contra el valor actual del catalogo.
    public void editarCurso(String id, String nombre, int creditos, int horas) throws IOException {
        List<Curso> cursos = new ArrayList<>(cursoRepo.cargar().registros());
        int indice = indiceDe(cursos, c -> c.id().equals(id));
        if (indice < 0) throw new OperacionInvalidaException("No existe un curso con ese ID.");

        String nombreLimpio = validarNombre(nombre);
        List<String> otrosNombres = cursos.stream()
            .filter(c -> !c.id().equals(id)).map(Curso::nombre).toList();
        if (nombreDuplicado(otrosNombres, nombreLimpio)) {
            throw new OperacionInvalidaException("Ya existe un curso con ese nombre.");
        }
        validarCreditosYHoras(creditos, horas);

        cursos.set(indice, new Curso(id, nombreLimpio, creditos, horas));
        cursoRepo.guardarTodos(cursos);
    }

    // Con incluirNotas=false se bloquea si hay notas que lo referencian; con true las borra
    // junto con el curso. Devuelve cuantas notas se eliminaron.
    public int eliminarCurso(String id, boolean incluirNotas) throws IOException {
        List<Curso> cursos = new ArrayList<>(cursoRepo.cargar().registros());
        if (cursos.stream().noneMatch(c -> c.id().equals(id))) {
            throw new OperacionInvalidaException("No existe un curso con ese ID.");
        }
        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        int notasAsociadas = (int) notas.stream().filter(n -> n.cursoId().equals(id)).count();
        if (notasAsociadas > 0 && !incluirNotas) {
            throw new OperacionInvalidaException("No se puede eliminar: hay " + notasAsociadas
                + " nota(s) asociada(s) a este curso. Eliminelas primero o elimine el curso junto con sus notas.");
        }
        cursos.removeIf(c -> c.id().equals(id));
        cursoRepo.guardarTodos(cursos);
        if (notasAsociadas > 0) {
            notas.removeIf(n -> n.cursoId().equals(id));
            notaRepo.guardarTodos(notas);
        }
        return notasAsociadas;
    }

    // Simetrica a la de alumnos: una nota pseudoaleatoria en el curso para cada alumno.
    public ResultadoGeneracion generarNotasCurso(String id) throws IOException {
        if (cursoRepo.cargar().registros().stream().noneMatch(c -> c.id().equals(id))) {
            throw new OperacionInvalidaException("No existe un curso con ese ID.");
        }
        List<Alumno> alumnos = alumnoRepo.cargar().registros();
        if (alumnos.isEmpty()) {
            throw new OperacionInvalidaException("No hay alumnos registrados. Cree al menos un alumno primero.");
        }

        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        boolean teniaNotas = notas.removeIf(n -> n.cursoId().equals(id));
        List<NotaCurso> notasNuevas = generador.generarNotasAleatoriasParaCurso(id, alumnos);
        notas.addAll(notasNuevas);
        notaRepo.guardarTodos(notas);
        return new ResultadoGeneracion(notasNuevas.size(), teniaNotas);
    }

    // ---------- Ficha ----------

    public Ficha fichaAlumno(String id) throws IOException {
        Base base = cargarBase();
        Alumno alumno = base.alumnos().stream().filter(a -> a.id().equals(id)).findFirst()
            .orElseThrow(() -> new OperacionInvalidaException("No existe un alumno con ese ID."));
        PromedioAlumno enRanking = calcularRanking(base).registros().stream()
            .filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        Map<String, Curso> cursosPorId = mapPorId(base.cursos(), Curso::id);

        List<NotaFicha> notas = new ArrayList<>();
        int horas = 0;
        for (NotaCurso n : base.notas()) {
            Curso curso = cursosPorId.get(n.cursoId());
            if (!n.alumnoId().equals(id) || curso == null) continue;
            horas += curso.horasSemanales();
            notas.add(new NotaFicha(curso.id(), curso.nombre(), curso.creditos(), n.nota(),
                n.nota() * curso.creditos(), PromedioService.estaAprobada(n.nota())));
        }

        double promedio = enRanking == null ? 0.0 : enRanking.getPromedio();
        return new Ficha(alumno, promedio, enRanking == null ? null : enRanking.getPuesto(),
            enRanking == null ? 0 : enRanking.getSumaCreditos(), horas,
            (int) notas.stream().filter(n -> !n.aprobada()).count(),
            PromedioService.aplicaBeca(promedio), PromedioService.esCuadroDeHonor(promedio),
            PromedioService.faltaPara(PromedioService.UMBRAL_BECA, promedio),
            PromedioService.faltaPara(PromedioService.UMBRAL_HONOR, promedio), notas);
    }

    // ---------- Notas ----------

    // Un alumno no puede tener dos notas en el mismo curso.
    public void agregarNota(String alumnoId, String cursoId, double nota) throws IOException {
        if (alumnoRepo.cargar().registros().stream().noneMatch(a -> a.id().equals(alumnoId))) {
            throw new OperacionInvalidaException("No existe un alumno con ese ID.");
        }
        if (cursoRepo.cargar().registros().stream().noneMatch(c -> c.id().equals(cursoId))) {
            throw new OperacionInvalidaException("No existe un curso con ese ID.");
        }
        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        if (notas.stream().anyMatch(n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId))) {
            throw new OperacionInvalidaException("Ya existe una nota para ese alumno en ese curso. Use Editar en su lugar.");
        }
        validarNota(nota);

        notas.add(new NotaCurso(alumnoId, cursoId, nota));
        notaRepo.guardarTodos(notas);
    }

    public void editarNota(String alumnoId, String cursoId, double nuevaNota) throws IOException {
        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        int indice = indiceDe(notas, n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId));
        if (indice < 0) throw new OperacionInvalidaException("No existe una nota para ese alumno en ese curso.");
        validarNota(nuevaNota);

        notas.set(indice, new NotaCurso(alumnoId, cursoId, nuevaNota));
        notaRepo.guardarTodos(notas);
    }

    public void eliminarNota(String alumnoId, String cursoId) throws IOException {
        List<NotaCurso> notas = new ArrayList<>(notaRepo.cargar().registros());
        if (!notas.removeIf(n -> n.alumnoId().equals(alumnoId) && n.cursoId().equals(cursoId))) {
            throw new OperacionInvalidaException("No existe una nota para ese alumno en ese curso.");
        }
        notaRepo.guardarTodos(notas);
    }

    // ---------- Validaciones ----------

    // El ';' es el separador de los archivos, no puede ir dentro de un nombre.
    private String validarNombre(String nombre) {
        String limpio = normalizar(nombre);
        if (limpio.isEmpty()) throw new OperacionInvalidaException("El nombre no puede estar vacio.");
        if (limpio.contains(";")) throw new OperacionInvalidaException("El nombre no puede contener ';'.");
        return limpio;
    }

    private void validarCreditosYHoras(int creditos, int horas) {
        if (creditos <= 0) throw new OperacionInvalidaException("Creditos invalidos, debe ser un entero positivo.");
        if (horas <= 0) throw new OperacionInvalidaException("Horas invalidas, debe ser un entero positivo.");
    }

    private void validarNota(double nota) {
        if (Double.isNaN(nota) || nota < 0.0 || nota > 5.0) {
            throw new OperacionInvalidaException("Nota invalida, debe ser un numero entre 0.0 y 5.0.");
        }
    }

    // Compara ignorando mayusculas/minusculas y espacios repetidos, para no permitir el mismo
    // nombre en dos variantes ("Ana  Perez" y "ana perez" cuentan como el mismo).
    private boolean nombreDuplicado(List<String> nombresExistentes, String nombreNuevo) {
        return nombresExistentes.stream().anyMatch(n -> normalizar(n).equalsIgnoreCase(normalizar(nombreNuevo)));
    }

    private static String normalizar(String nombre) {
        return nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
    }

    private <T> int indiceDe(List<T> lista, Predicate<T> condicion) {
        for (int i = 0; i < lista.size(); i++) {
            if (condicion.test(lista.get(i))) return i;
        }
        return -1;
    }

    // Numero mas alto entre los IDs existentes con ese prefijo, mas uno.
    private String generarSiguienteId(List<String> idsExistentes, String prefijo) {
        int maximo = 0;
        for (String id : idsExistentes) {
            if (!id.startsWith(prefijo)) continue;
            try {
                maximo = Math.max(maximo, Integer.parseInt(id.substring(prefijo.length())));
            } catch (NumberFormatException ignorado) {
                // Formato distinto al esperado, se ignora.
            }
        }
        return prefijo + (maximo + 1);
    }
}
