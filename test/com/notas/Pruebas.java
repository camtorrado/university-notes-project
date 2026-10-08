package com.notas;

import com.notas.model.Alumno;
import com.notas.model.Curso;
import com.notas.model.NotaCurso;
import com.notas.model.PromedioAlumno;
import com.notas.model.ResultadoCarga;
import com.notas.repository.AlumnoRepository;
import com.notas.repository.CursoRepository;
import com.notas.repository.NotaRepository;
import com.notas.service.GeneradorDatosService;
import com.notas.service.GestionService;
import com.notas.service.GestionService.EstadoSalida;
import com.notas.service.GestionService.Ficha;
import com.notas.service.OperacionInvalidaException;
import com.notas.service.PromedioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

// Pruebas de la logica sin librerias externas. Cada prueba trabaja en una carpeta temporal,
// nunca en data/. Termina con codigo 1 si alguna falla.
//   javac -d bin $(find src test -name "*.java") && java -cp bin com.notas.Pruebas
public class Pruebas {

    @FunctionalInterface
    private interface Prueba {
        void ejecutar() throws Exception;
    }

    private static int aprobadas = 0;
    private static int fallidas = 0;

    public static void main(String[] args) {
        System.out.println("--- Calculo ---");
        prueba("promedio ponderado por creditos", Pruebas::promedioPonderado);
        prueba("alumno sin notas tiene promedio 0.0 y no entra al ranking", Pruebas::alumnoSinNotas);
        prueba("limites de beca y cuadro de honor", Pruebas::limitesBecaYHonor);
        prueba("redondeo a 2 decimales decide la beca", Pruebas::redondeoDecideBeca);
        prueba("empates comparten puesto (1, 2, 2, 4)", Pruebas::empatesCompartenPuesto);
        prueba("en empate va primero quien curso mas creditos", Pruebas::desempatePorCreditos);
        prueba("carga horaria suma las horas de los cursos con nota", Pruebas::cargaHoraria);
        prueba("referencias rotas quedan como advertencia", Pruebas::referenciasRotas);

        System.out.println("--- Lectura de archivos ---");
        prueba("linea con formato invalido se ignora", Pruebas::lineaInvalida);
        prueba("nota fuera de rango se ignora", Pruebas::notaFueraDeRango);
        prueba("ID duplicado se ignora", Pruebas::idDuplicado);
        prueba("par alumno-curso duplicado cuenta una sola vez", Pruebas::parDuplicado);
        prueba("guardar y leer con idioma es-CO conserva el punto decimal", Pruebas::idiomaEspanol);

        System.out.println("--- CRUD ---");
        prueba("nombre duplicado sin importar mayusculas ni espacios", Pruebas::nombreDuplicado);
        prueba("nombre vacio o con ';' se rechaza", Pruebas::nombreInvalido);
        prueba("IDs automaticos continuan desde el mayor (A1, A5 -> A6)", Pruebas::idsAutomaticos);
        prueba("eliminar alumno borra sus notas", Pruebas::cascadaAlumno);
        prueba("eliminar curso con notas: bloqueado o en cascada", Pruebas::eliminarCurso);
        prueba("un alumno no puede tener dos notas en el mismo curso", Pruebas::parUnico);
        prueba("editar nota valida el rango", Pruebas::editarNota);
        prueba("ficha del alumno", Pruebas::ficha);

        System.out.println("--- Salida ---");
        prueba("estado de promedios.csv: no generada, al dia, desactualizada", Pruebas::estadoSalida);
        prueba("promedios.csv usa el formato X.XX_Prom y el puesto compartido", Pruebas::formatoSalida);
        prueba("exportar sin datos se rechaza", Pruebas::exportarSinDatos);

        System.out.println("--- Generador ---");
        prueba("datos generados son coherentes y sin pares repetidos", Pruebas::generadorCoherente);

        System.out.printf("%n%d aprobadas, %d fallidas%n", aprobadas, fallidas);
        System.exit(fallidas == 0 ? 0 : 1);
    }

    // ---------- Calculo ----------

    private static void promedioPonderado() throws Exception {
        GestionService g = conDatos(
            List.of("A1;Ana"),
            List.of("C1;Calculo;3_creditos;4_horas", "C2;Ingles;1_creditos;2_horas"),
            List.of("A1;C1;4.0", "A1;C2;2.0"));
        // (4.0*3 + 2.0*1) / 4 = 3.5
        PromedioAlumno p = g.calcularRanking().registros().get(0);
        igual(3.5, p.getPromedio(), "promedio");
        igual(4, p.getSumaCreditos(), "creditos");
    }

    private static void alumnoSinNotas() throws Exception {
        igual(0.0, new PromedioAlumno("A1", "Ana").getPromedio(), "promedio sin notas");
        GestionService g = conDatos(List.of("A1;Ana", "A2;Beto"), List.of("C1;Calculo;3_creditos;4_horas"), List.of("A1;C1;4.0"));
        GestionService.Base base = g.cargarBase();
        List<PromedioAlumno> ranking = g.calcularRanking(base).registros();
        igual(1, ranking.size(), "solo entra quien tiene notas");
        igual(1L, GestionService.contarSinNotas(base, ranking), "alumnos sin notas");
    }

    private static void limitesBecaYHonor() {
        verificar(!PromedioService.aplicaBeca(3.99), "3.99 no aplica a beca");
        verificar(PromedioService.aplicaBeca(4.0), "4.0 aplica a beca");
        verificar(!PromedioService.esCuadroDeHonor(4.49), "4.49 no es cuadro de honor");
        verificar(PromedioService.esCuadroDeHonor(4.5), "4.5 es cuadro de honor");
        verificar(PromedioService.estaAprobada(3.0) && !PromedioService.estaAprobada(2.9), "nota aprobatoria 3.0");
        igual(0.12, PromedioService.faltaPara(4.0, 3.88), "falta para beca");
        igual(0.0, PromedioService.faltaPara(4.0, 4.2), "no falta nada");
    }

    private static void redondeoDecideBeca() {
        // 3.995 exacto se muestra como 4.00, asi que debe aplicar a beca (no contradecir la pantalla).
        PromedioAlumno p = new PromedioAlumno("A1", "Ana");
        p.agregarNota(3.99, 1);
        p.agregarNota(4.0, 1);
        igual(4.0, p.getPromedio(), "3.995 redondea a 4.00");
        verificar(PromedioService.aplicaBeca(p.getPromedio()), "con 4.00 aplica a beca");
    }

    private static void empatesCompartenPuesto() throws Exception {
        GestionService g = conDatos(
            List.of("A1;Ana", "A2;Beto", "A3;Carla", "A4;Dario"),
            List.of("C1;Calculo;3_creditos;4_horas"),
            List.of("A1;C1;5.0", "A2;C1;4.0", "A3;C1;4.0", "A4;C1;3.0"));
        List<Integer> puestos = g.calcularRanking().registros().stream().map(PromedioAlumno::getPuesto).toList();
        igual(List.of(1, 2, 2, 4), puestos, "puestos");
    }

    private static void desempatePorCreditos() throws Exception {
        GestionService g = conDatos(
            List.of("A1;Ana", "A2;Beto"),
            List.of("C1;Calculo;3_creditos;4_horas", "C2;Ingles;1_creditos;2_horas"),
            List.of("A1;C2;4.0", "A2;C1;4.0", "A2;C2;4.0"));
        List<PromedioAlumno> ranking = g.calcularRanking().registros();
        igual("A2", ranking.get(0).getId(), "primero quien tiene mas creditos");
        igual(ranking.get(0).getPuesto(), ranking.get(1).getPuesto(), "mismo puesto");
    }

    private static void cargaHoraria() throws Exception {
        GestionService g = conDatos(
            List.of("A1;Ana"),
            List.of("C1;Calculo;3_creditos;4_horas", "C2;Ingles;1_creditos;2_horas"),
            List.of("A1;C1;4.0", "A1;C2;3.0"));
        GestionService.Base base = g.cargarBase();
        Map<String, Integer> horas = GestionService.calcularHorasPorAlumno(
            GestionService.mapPorId(base.cursos(), Curso::id), base.notas());
        igual(6, horas.get("A1"), "horas semanales");
    }

    private static void referenciasRotas() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"), List.of("C1;Calculo;3_creditos;4_horas"),
            List.of("A1;C1;4.0", "A9;C1;3.0", "A1;C9;2.0"));
        ResultadoCarga<PromedioAlumno> r = g.calcularRanking();
        igual(4.0, r.registros().get(0).getPromedio(), "las notas rotas no cuentan");
        igual(2, r.errores().size(), "advertencias");
    }

    // ---------- Lectura de archivos ----------

    private static void lineaInvalida() throws Exception {
        Path dir = carpeta();
        Files.writeString(dir.resolve("alumnos.csv"), "A1;Ana\nsin separador\n\nA2;Beto\n");
        ResultadoCarga<Alumno> r = new AlumnoRepository(dir.resolve("alumnos.csv")).cargar();
        igual(2, r.registros().size(), "alumnos validos");
        igual(1, r.errores().size(), "errores");
        verificar(r.errores().get(0).contains("linea 2"), "el error indica la linea");
    }

    private static void notaFueraDeRango() throws Exception {
        Path dir = carpeta();
        Files.writeString(dir.resolve("notas.txt"), "A1;C1;5.1\nA1;C2;4.0\n");
        ResultadoCarga<NotaCurso> r = new NotaRepository(dir.resolve("notas.txt")).cargar();
        igual(1, r.registros().size(), "notas validas");
        verificar(r.errores().get(0).contains("fuera de rango"), "mensaje de rango");
    }

    private static void idDuplicado() throws Exception {
        Path dir = carpeta();
        Files.writeString(dir.resolve("cursos.csv"), "C1;Calculo;3_creditos;4_horas\nC1;Fisica;2_creditos;2_horas\n");
        ResultadoCarga<Curso> r = new CursoRepository(dir.resolve("cursos.csv")).cargar();
        igual(1, r.registros().size(), "cursos validos");
        igual("Calculo", r.registros().get(0).nombre(), "se conserva el primero");
    }

    private static void parDuplicado() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"), List.of("C1;Calculo;3_creditos;4_horas"),
            List.of("A1;C1;5.0", "A1;C1;1.0"));
        ResultadoCarga<PromedioAlumno> r = g.calcularRanking();
        igual(5.0, r.registros().get(0).getPromedio(), "solo cuenta la primera nota");
        verificar(r.errores().stream().anyMatch(e -> e.contains("ya tiene nota")), "advertencia del duplicado");
    }

    private static void idiomaEspanol() throws Exception {
        Locale anterior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("es-CO"));
            Path dir = carpeta();
            NotaRepository repo = new NotaRepository(dir.resolve("notas.txt"));
            repo.guardarTodos(List.of(new NotaCurso("A1", "C1", 4.5)));
            igual("A1;C1;4.5", Files.readString(dir.resolve("notas.txt")).trim(), "contenido del archivo");
            ResultadoCarga<NotaCurso> r = repo.cargar();
            igual(0, r.errores().size(), "se vuelve a leer sin errores");
            igual(4.5, r.registros().get(0).nota(), "nota leida");
        } finally {
            Locale.setDefault(anterior);
        }
    }

    // ---------- CRUD ----------

    private static void nombreDuplicado() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana Perez"), List.of(), List.of());
        falla(() -> g.agregarAlumno("ana perez"), "mayusculas distintas");
        falla(() -> g.agregarAlumno("  Ana   Perez "), "espacios repetidos");
        String id = g.agregarAlumno("  Beto   Gomez ");
        igual("Beto Gomez", g.cargarBase().alumnos().stream().filter(a -> a.id().equals(id)).findFirst()
            .orElseThrow().nombreCompleto(), "el nombre se guarda normalizado");
        g.editarAlumno("A1", "ANA PEREZ");
    }

    private static void nombreInvalido() throws Exception {
        GestionService g = conDatos(List.of(), List.of(), List.of());
        falla(() -> g.agregarAlumno("   "), "vacio");
        falla(() -> g.agregarAlumno("Ana;Perez"), "con separador");
        falla(() -> g.agregarCurso("Redes", 0, 2), "creditos en cero");
        falla(() -> g.agregarCurso("Redes", 2, -1), "horas negativas");
    }

    private static void idsAutomaticos() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana", "A5;Beto"), List.of(), List.of());
        igual("A6", g.agregarAlumno("Carla"), "siguiente ID");
        igual("C1", g.agregarCurso("Calculo", 3, 4), "primer curso");
    }

    private static void cascadaAlumno() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana", "A2;Beto"), List.of("C1;Calculo;3_creditos;4_horas"),
            List.of("A1;C1;4.0", "A2;C1;3.0"));
        igual(1, g.eliminarAlumno("A1"), "notas eliminadas");
        igual(1, g.cargarBase().notas().size(), "quedan las del otro alumno");
        falla(() -> g.eliminarAlumno("A1"), "ya no existe");
    }

    private static void eliminarCurso() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"),
            List.of("C1;Calculo;3_creditos;4_horas", "C2;Ingles;1_creditos;2_horas"),
            List.of("A1;C1;4.0", "A1;C2;3.0"));
        falla(() -> g.eliminarCurso("C1", false), "bloqueado si tiene notas");
        igual(2, g.cargarBase().cursos().size(), "no se borro nada");
        igual(1, g.eliminarCurso("C1", true), "en cascada borra sus notas");
        igual(List.of("C2"), g.cargarBase().notas().stream().map(NotaCurso::cursoId).toList(), "notas restantes");
    }

    private static void parUnico() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"), List.of("C1;Calculo;3_creditos;4_horas"), List.of("A1;C1;4.0"));
        falla(() -> g.agregarNota("A1", "C1", 3.0), "par repetido");
        falla(() -> g.agregarNota("A9", "C1", 3.0), "alumno inexistente");
        falla(() -> g.agregarNota("A1", "C9", 3.0), "curso inexistente");
    }

    private static void editarNota() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"), List.of("C1;Calculo;3_creditos;4_horas"), List.of("A1;C1;4.0"));
        falla(() -> g.editarNota("A1", "C1", 5.5), "fuera de rango");
        g.editarNota("A1", "C1", 2.5);
        igual(2.5, g.cargarBase().notas().get(0).nota(), "nota editada");
    }

    private static void ficha() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana", "A2;Beto"),
            List.of("C1;Calculo;3_creditos;4_horas", "C2;Ingles;1_creditos;2_horas"),
            List.of("A1;C1;4.0", "A1;C2;2.0"));
        Ficha f = g.fichaAlumno("A1");
        igual(3.5, f.promedio(), "promedio");
        igual(1, f.puesto(), "puesto");
        igual(6, f.horas(), "horas");
        igual(1, f.reprobados(), "reprobadas");
        igual(0.5, f.faltaBeca(), "falta para beca");
        igual(12.0, f.notas().get(0).aporte(), "aporte = nota x creditos");
        verificar(g.fichaAlumno("A2").puesto() == null, "sin notas no tiene puesto");
        falla(() -> g.fichaAlumno("A9"), "alumno inexistente");
    }

    // ---------- Salida ----------

    private static void estadoSalida() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"), List.of("C1;Calculo;3_creditos;4_horas"), List.of("A1;C1;4.0"));
        Path dir = g.getDirDatos();
        // Fechas fijas: no depende de la precision del reloj del sistema de archivos.
        for (String archivo : List.of("alumnos.csv", "cursos.csv", "notas.txt")) {
            Files.setLastModifiedTime(dir.resolve(archivo), FileTime.fromMillis(1_000_000));
        }
        igual(EstadoSalida.NO_GENERADA, g.estadoSalida(), "antes de exportar");
        g.exportarRanking();
        Files.setLastModifiedTime(dir.resolve("promedios.csv"), FileTime.fromMillis(2_000_000));
        igual(EstadoSalida.AL_DIA, g.estadoSalida(), "recien exportada");
        Files.setLastModifiedTime(dir.resolve("notas.txt"), FileTime.fromMillis(3_000_000));
        igual(EstadoSalida.DESACTUALIZADA, g.estadoSalida(), "cambio despues de exportar");
        verificar(g.limpiarSalida(), "limpiar elimina el archivo");
        igual(EstadoSalida.NO_GENERADA, g.estadoSalida(), "despues de limpiar");
    }

    private static void formatoSalida() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana", "A2;Beto"),
            List.of("C1;Calculo;3_creditos;4_horas"),
            List.of("A1;C1;4.6", "A2;C1;4.6"));
        Path ruta = g.exportarRanking();
        List<String> lineas = Files.readAllLines(ruta);
        igual("1_Puesto;A1;Ana;4.60_Prom;SI_Beca;SI_Honor;4_HorasSemanales", lineas.get(0), "primera linea");
        verificar(lineas.get(1).startsWith("1_Puesto;A2;"), "el empate comparte puesto");
    }

    private static void exportarSinDatos() throws Exception {
        GestionService g = conDatos(List.of("A1;Ana"), List.of(), List.of());
        falla(g::exportarRanking, "sin notas no hay ranking");
    }

    // ---------- Generador ----------

    private static void generadorCoherente() {
        GeneradorDatosService.DatosGenerados d = new GeneradorDatosService(new Random(42)).generarEjemplos(30, 4);
        igual(30, d.alumnos().size(), "alumnos");
        igual(4, d.cursos().size(), "cursos");
        Set<String> alumnos = new HashSet<>(d.alumnos().stream().map(Alumno::id).toList());
        Set<String> cursos = new HashSet<>(d.cursos().stream().map(Curso::id).toList());
        Set<String> pares = new HashSet<>();
        for (NotaCurso n : d.notas()) {
            verificar(alumnos.contains(n.alumnoId()) && cursos.contains(n.cursoId()), "referencia valida " + n);
            verificar(pares.add(n.alumnoId() + ";" + n.cursoId()), "par repetido " + n);
            verificar(n.nota() >= 0.0 && n.nota() <= 5.0, "nota en rango " + n);
        }
        Set<String> nombres = new HashSet<>();
        d.alumnos().forEach(a -> verificar(nombres.add(a.nombreCompleto().toLowerCase(Locale.ROOT)), "nombre repetido " + a));
    }

    // ---------- Utilidades ----------

    private static void prueba(String nombre, Prueba prueba) {
        try {
            prueba.ejecutar();
            aprobadas++;
            System.out.println("  OK     " + nombre);
        } catch (AssertionError e) {
            fallidas++;
            System.out.println("  FALLA  " + nombre + ": " + e.getMessage());
        } catch (Exception e) {
            fallidas++;
            System.out.println("  ERROR  " + nombre + ": " + e);
        }
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void igual(Object esperado, Object obtenido, String mensaje) {
        if (!esperado.equals(obtenido)) {
            throw new AssertionError(mensaje + " (esperado " + esperado + ", obtenido " + obtenido + ")");
        }
    }

    // La accion debe ser rechazada por una regla de negocio.
    private static void falla(Prueba accion, String caso) throws Exception {
        try {
            accion.ejecutar();
        } catch (OperacionInvalidaException esperada) {
            return;
        }
        throw new AssertionError("deberia rechazarse: " + caso);
    }

    private static Path carpeta() throws IOException {
        Path dir = Files.createTempDirectory("notas-pruebas");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> borrar(dir)));
        return dir;
    }

    private static GestionService conDatos(List<String> alumnos, List<String> cursos, List<String> notas) throws IOException {
        Path dir = carpeta();
        Files.write(dir.resolve("alumnos.csv"), alumnos);
        Files.write(dir.resolve("cursos.csv"), cursos);
        Files.write(dir.resolve("notas.txt"), notas);
        return new GestionService(dir);
    }

    private static void borrar(Path dir) {
        try (Stream<Path> rutas = Files.walk(dir)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignorado) {
            // Carpeta temporal: si no se puede borrar, la limpia el sistema.
        }
    }
}
