package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.exception.RecursoNoEncontradoException;
import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.model.Ruta;
import com.perfumeria.logistica.model.TipoNodo;
import com.perfumeria.logistica.repository.GrafoRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Punto único de carga del grafo: lee Neo4j UNA vez por solicitud, arma el {@link GrafoMemoria} y
 * se lo entrega a los algoritmos. Los algoritmos nunca reciben el repositorio.
 */
@Service
public class GrafoService {

    private final GrafoRepository repositorio;

    public GrafoService(GrafoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public record ResumenGrafo(int vertices, int aristas, int paquetes, Map<String, Integer> nodosPorTipo,
                               String unidadCosto, boolean dirigido) {
    }

    public List<Nodo> nodos() {
        return repositorio.obtenerNodos();
    }

    public List<Ruta> rutas() {
        return repositorio.obtenerRutas();
    }

    public List<Paquete> paquetes() {
        return repositorio.obtenerPaquetes();
    }

    public ResumenGrafo resumen() {
        List<Nodo> nodos = repositorio.obtenerNodos();
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        for (TipoNodo t : TipoNodo.values()) {
            porTipo.put(t.name(), 0);
        }
        for (Nodo n : nodos) {
            porTipo.merge(n.tipo().name(), 1, Integer::sum);
        }
        return new ResumenGrafo(nodos.size(), repositorio.obtenerRutas().size(), repositorio.obtenerPaquetes().size(),
                porTipo, "minutos", false);
    }

    /** Lee nodos y rutas de Neo4j y los convierte a la estructura en memoria que usan los algoritmos. */
    public GrafoMemoria cargarGrafo() {
        List<Nodo> nodos = repositorio.obtenerNodos();
        if (nodos.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "El grafo está vacío: cargá los datos en Neo4j (la semilla se carga sola al arrancar con la base vacía)");
        }
        return GrafoMemoria.desde(nodos, repositorio.obtenerRutas());
    }

    /** Índice del nodo en el grafo; acepta "la plata", "La_Plata" o "LA_PLATA" y tolera tildes. */
    public int resolverNodo(GrafoMemoria grafo, String id) {
        int indice = grafo.indice(normalizarId(id));
        if (indice < 0) {
            throw new RecursoNoEncontradoException(
                    "El nodo '" + id + "' no existe. Nodos disponibles: " + grafo.ids());
        }
        return indice;
    }

    static String normalizarId(String id) {
        if (id == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(id.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replace(' ', '_');
    }
}
