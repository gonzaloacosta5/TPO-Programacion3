package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recorridos de grafo: BFS (anchura) y DFS (profundidad).
 *
 * <p>Ambos visitan los vecinos en el orden de la lista de adyacencia, que {@link GrafoMemoria}
 * deja ordenada por índice de vértice (= orden alfabético de ids): el resultado es determinista.</p>
 */
public final class Recorridos {

    private Recorridos() {
    }

    /**
     * @param saltosDesdeOrigen cantidad MÍNIMA de aristas desde el origen (BFS en grafo sin pesos)
     * @param padres            nodo desde el que se descubrió cada nodo (el origen no figura)
     */
    public record ResultadoBfs(String algoritmo, String origen, List<String> ordenVisita,
                               Map<String, Integer> saltosDesdeOrigen, Map<String, String> padres,
                               int nodosVisitados, int nodosNoAlcanzados) {
    }

    /**
     * @param ordenVisita       orden de descubrimiento (preorden)
     * @param ordenFinalizacion orden en que cada nodo terminó de explorarse (postorden)
     */
    public record ResultadoDfs(String algoritmo, String origen, List<String> ordenVisita,
                               List<String> ordenFinalizacion, Map<String, String> padres,
                               int nodosVisitados, int nodosNoAlcanzados) {
    }

    // ------------------------------------------------------------------ BFS

    /**
     * BFS con una cola implementada sobre un arreglo de tamaño V (cada vértice entra una sola vez).
     * Tiempo O(V + E): cada vértice se encola/desencola una vez y cada lista de adyacencia se
     * recorre una vez. Espacio O(V) extra (cola + visitados + padres).
     */
    public static ResultadoBfs bfs(GrafoMemoria g, int origen) {
        int n = g.numVertices();
        int[] cola = new int[n];
        int cabeza = 0;
        int fin = 0;
        boolean[] visitado = new boolean[n];
        int[] padre = new int[n];
        int[] saltos = new int[n];
        for (int i = 0; i < n; i++) {
            padre[i] = -1;
            saltos[i] = -1;
        }

        visitado[origen] = true;
        saltos[origen] = 0;
        cola[fin++] = origen;

        List<String> orden = new ArrayList<>();
        Map<String, Integer> saltosPorNodo = new LinkedHashMap<>();
        Map<String, String> padres = new LinkedHashMap<>();

        while (cabeza < fin) {
            int u = cola[cabeza++];
            orden.add(g.id(u));
            saltosPorNodo.put(g.id(u), saltos[u]);
            if (padre[u] >= 0) {
                padres.put(g.id(u), g.id(padre[u]));
            }
            for (GrafoMemoria.Arista a : g.vecinos(u)) {
                int v = a.destino();
                if (!visitado[v]) {
                    visitado[v] = true;
                    padre[v] = u;
                    saltos[v] = saltos[u] + 1;
                    cola[fin++] = v;
                }
            }
        }
        return new ResultadoBfs("BFS", g.id(origen), orden, saltosPorNodo, padres, orden.size(), n - orden.size());
    }

    // ------------------------------------------------------------------ DFS

    /**
     * DFS recursivo. Tiempo O(V + E). Espacio O(V): visitados + padres + pila de recursión,
     * que en el peor caso (un camino) llega a profundidad V.
     */
    public static ResultadoDfs dfs(GrafoMemoria g, int origen) {
        int n = g.numVertices();
        EstadoDfs e = new EstadoDfs(n);
        visitar(g, origen, e);

        List<String> orden = new ArrayList<>();
        List<String> finalizacion = new ArrayList<>();
        Map<String, String> padres = new LinkedHashMap<>();
        for (int u : e.preorden) {
            orden.add(g.id(u));
            if (e.padre[u] >= 0) {
                padres.put(g.id(u), g.id(e.padre[u]));
            }
        }
        for (int u : e.postorden) {
            finalizacion.add(g.id(u));
        }
        return new ResultadoDfs("DFS", g.id(origen), orden, finalizacion, padres, orden.size(), n - orden.size());
    }

    private static void visitar(GrafoMemoria g, int u, EstadoDfs e) {
        e.visitado[u] = true;
        e.preorden.add(u);
        for (GrafoMemoria.Arista a : g.vecinos(u)) {
            int v = a.destino();
            if (!e.visitado[v]) {
                e.padre[v] = u;
                visitar(g, v, e);
            }
        }
        e.postorden.add(u);
    }

    private static final class EstadoDfs {
        final boolean[] visitado;
        final int[] padre;
        final List<Integer> preorden = new ArrayList<>();
        final List<Integer> postorden = new ArrayList<>();

        EstadoDfs(int n) {
            visitado = new boolean[n];
            padre = new int[n];
            for (int i = 0; i < n; i++) {
                padre[i] = -1;
            }
        }
    }
}
