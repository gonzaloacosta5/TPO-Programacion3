package com.perfumeria.logistica.arquitectura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Convierte en tests las reglas de la consigna que más descuentan puntos, leyendo el código fuente:
 * <ul>
 *   <li>los controllers no contienen lógica algorítmica ni tocan algoritmos/repositorios;</li>
 *   <li>los algoritmos no acceden a la base ni a Spring y no usan atajos prohibidos (sort de la librería,
 *       PriorityQueue, streams que ordenan);</li>
 *   <li>no hay credenciales en el código ni en application.properties.</li>
 * </ul>
 */
class ArquitecturaTest {

    private static final Path MAIN = Path.of("src", "main");
    private static final Path JAVA = MAIN.resolve(Path.of("java", "com", "perfumeria", "logistica"));

    @Test
    @DisplayName("Los controllers no importan el paquete algorithm ni el repository (solo hablan con services)")
    void controllersSoloHablanConServices() throws IOException {
        List<String> infracciones = new ArrayList<>();
        for (Path archivo : archivosJava(JAVA.resolve("controller"))) {
            for (String linea : Files.readAllLines(archivo)) {
                if (linea.startsWith("import ") && (linea.contains(".repository.") || linea.contains(".algorithm.estructura.")
                        || linea.contains(".algorithm.ordenamiento."))) {
                    infracciones.add(archivo.getFileName() + ": " + linea);
                }
                if (linea.contains("new MinHeap") || linea.contains("new UnionFind") || linea.contains("Ordenamiento.")) {
                    infracciones.add(archivo.getFileName() + ": " + linea.trim());
                }
            }
        }
        assertEquals(List.of(), infracciones);
    }

    @Test
    @DisplayName("Los controllers son finos: sin bucles ni condicionales")
    void controllersSinLogica() throws IOException {
        List<String> infracciones = new ArrayList<>();
        for (Path archivo : archivosJava(JAVA.resolve("controller"))) {
            for (String linea : Files.readAllLines(archivo)) {
                String t = linea.trim();
                if (t.startsWith("for (") || t.startsWith("while (") || t.startsWith("if (") || t.startsWith("switch (")) {
                    infracciones.add(archivo.getFileName() + ": " + t);
                }
            }
        }
        assertEquals(List.of(), infracciones);
    }

    @Test
    @DisplayName("El paquete algorithm no depende de Spring, Neo4j, repository, service ni controller")
    void algoritmosNoTocanLaBase() throws IOException {
        List<String> prohibidos = List.of("org.springframework", "org.neo4j", ".repository.", ".service.",
                ".controller.", "Neo4jClient", "Driver", "Session");
        List<String> infracciones = new ArrayList<>();
        for (Path archivo : archivosJava(JAVA.resolve("algorithm"))) {
            for (String linea : Files.readAllLines(archivo)) {
                if (linea.startsWith("import ")) {
                    for (String p : prohibidos) {
                        if (linea.contains(p)) {
                            infracciones.add(archivo.getFileName() + ": " + linea);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), infracciones);
    }

    @Test
    @DisplayName("Los algoritmos no usan sort de la librería, PriorityQueue, ni librerías de grafos")
    void sinAtajosProhibidos() throws IOException {
        List<String> prohibidos = List.of("Collections.sort", "Arrays.sort", ".sort(", ".sorted(", "PriorityQueue",
                "TreeMap", "TreeSet", "parallelSort", "org.jgrapht", "org.apache.commons", "com.google.common",
                "org.neo4j.graphalgo", "gds.");
        List<String> infracciones = new ArrayList<>();
        for (Path archivo : archivosJava(JAVA.resolve("algorithm"))) {
            for (String linea : Files.readAllLines(archivo)) {
                String sinComentario = linea.trim();
                if (sinComentario.startsWith("*") || sinComentario.startsWith("//") || sinComentario.startsWith("/*")) {
                    continue; // la documentación puede mencionar lo que NO se usa
                }
                for (String p : prohibidos) {
                    if (linea.contains(p)) {
                        infracciones.add(archivo.getFileName() + ": " + sinComentario);
                    }
                }
            }
        }
        assertEquals(List.of(), infracciones);
    }

    @Test
    @DisplayName("El algoritmo nunca recibe un repositorio ni un servicio: solo el grafo en memoria")
    void firmasDeAlgoritmosSinBaseDeDatos() throws IOException {
        for (Path archivo : archivosJava(JAVA.resolve("algorithm"))) {
            String contenido = Files.readString(archivo);
            assertTrue(!contenido.contains("GrafoRepository") && !contenido.contains("GrafoService"),
                    archivo.getFileName() + " no debe conocer la capa de datos");
        }
    }

    @Test
    @DisplayName("No hay credenciales hardcodeadas: las properties usan variables de entorno sin valor por defecto")
    void sinCredenciales() throws IOException {
        String properties = Files.readString(MAIN.resolve(Path.of("resources", "application.properties")));
        for (String linea : properties.split("\\R")) {
            String t = linea.trim();
            if (t.startsWith("#") || t.isEmpty()) {
                continue;
            }
            if (t.contains("password") || t.contains("username") || t.contains("secret") || t.contains("token")) {
                assertTrue(t.matches(".*=\\$\\{[A-Z0-9_]+}$"),
                        "la credencial debe leerse de una variable de entorno SIN valor por defecto: " + t);
            }
        }
        for (Path archivo : archivosJava(JAVA)) {
            for (String linea : Files.readAllLines(archivo)) {
                String t = linea.trim().toLowerCase();
                boolean asignacionLiteral = (t.contains("password") || t.contains("passwd") || t.contains("secret")
                        || t.contains("apikey") || t.contains("api_key"))
                        && t.contains("=") && t.contains("\"") && !t.startsWith("*") && !t.startsWith("//");
                assertTrue(!asignacionLiteral, archivo.getFileName() + " parece contener una credencial: " + linea.trim());
            }
        }
    }

    private static List<Path> archivosJava(Path raiz) throws IOException {
        try (Stream<Path> caminos = Files.walk(raiz)) {
            return caminos.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }
}
