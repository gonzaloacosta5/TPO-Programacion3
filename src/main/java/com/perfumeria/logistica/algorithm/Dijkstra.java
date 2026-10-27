package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.algorithm.estructura.MinHeap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dijkstra con min-heap propio y reconstrucción del camino mediante el arreglo de padres.
 *
 * <p><b>Complejidad:</b> cada arco se examina una vez (O(E)) y puede insertar una entrada en el
 * heap; el heap llega a tener a lo sumo O(E) entradas, así que cada operación cuesta
 * O(log E) = O(log V) porque E &le; V². Total O((V + E) log V). Espacio O(V + E): arreglos de
 * tamaño V más el heap (con "lazy deletion" guarda entradas obsoletas, hasta O(E)).</p>
 *
 * <p>Requiere pesos no negativos; con un peso negativo lanza {@link IllegalArgumentException}
 * (para eso está Floyd-Warshall).</p>
 */
public final class Dijkstra {

    private Dijkstra() {
    }

    /**
     * Árbol de caminos mínimos desde {@code origen}.
     *
     * @param distancia          costo mínimo a cada vértice ({@link GrafoMemoria#INFINITO} si es inalcanzable)
     * @param padre              predecesor en el camino mínimo (-1 para el origen y los inalcanzables)
     * @param nodosExpandidos    vértices extraídos del heap y expandidos (finalizados)
     * @param aristasExaminadas  arcos recorridos al expandir
     */
    public record Arbol(int origen, long[] distancia, int[] padre, int nodosExpandidos, long aristasExaminadas) {
    }

    public record Tramo(String desde, String hasta, int minutos) {
    }

    public record ResultadoCamino(String origen, String destino, boolean alcanzable, Long costoMinutos, int saltos,
                                  List<String> camino, List<Tramo> tramos,
                                  int nodosExpandidos, long aristasExaminadas) {
    }

    /**
     * @param parada vértice en el que se puede cortar apenas queda finalizado, o -1 para
     *               calcular las distancias a todos los vértices
     */
    public static Arbol calcular(GrafoMemoria g, int origen, int parada) {
        if (g.tienePesosNegativos()) {
            throw new IllegalArgumentException(
                    "Dijkstra no admite aristas con peso negativo; usá Floyd-Warshall para ese caso");
        }
        int n = g.numVertices();
        long[] dist = new long[n];
        int[] padre = new int[n];
        boolean[] cerrado = new boolean[n];
        for (int i = 0; i < n; i++) {
            dist[i] = GrafoMemoria.INFINITO;
            padre[i] = -1;
        }

        MinHeap heap = new MinHeap();
        dist[origen] = 0;
        heap.insertar(0, origen);

        int expandidos = 0;
        long examinadas = 0;
        while (!heap.estaVacio()) {
            int u = heap.extraerMinimo().valor();
            if (cerrado[u]) {
                continue; // entrada obsoleta: u ya se finalizó con una distancia menor o igual
            }
            cerrado[u] = true;
            expandidos++;
            if (u == parada) {
                break;
            }
            for (GrafoMemoria.Arista a : g.vecinos(u)) {
                examinadas++;
                int v = a.destino();
                long candidata = dist[u] + a.peso();
                if (candidata < dist[v]) {
                    dist[v] = candidata;
                    padre[v] = u;
                    heap.insertar(candidata, v);
                }
            }
        }
        return new Arbol(origen, dist, padre, expandidos, examinadas);
    }

    /** Camino mínimo origen -> destino (corta apenas el destino queda finalizado). */
    public static ResultadoCamino caminoMinimo(GrafoMemoria g, int origen, int destino) {
        Arbol arbol = calcular(g, origen, destino);
        if (arbol.distancia()[destino] >= GrafoMemoria.INFINITO) {
            return new ResultadoCamino(g.id(origen), g.id(destino), false, null, 0,
                    List.of(), List.of(), arbol.nodosExpandidos(), arbol.aristasExaminadas());
        }
        List<Integer> indices = reconstruir(arbol, destino);
        List<String> camino = new ArrayList<>();
        List<Tramo> tramos = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            camino.add(g.id(indices.get(i)));
            if (i > 0) {
                int desde = indices.get(i - 1);
                int hasta = indices.get(i);
                tramos.add(new Tramo(g.id(desde), g.id(hasta),
                        (int) (arbol.distancia()[hasta] - arbol.distancia()[desde])));
            }
        }
        return new ResultadoCamino(g.id(origen), g.id(destino), true, arbol.distancia()[destino],
                indices.size() - 1, camino, tramos, arbol.nodosExpandidos(), arbol.aristasExaminadas());
    }

    /** Reconstruye origen -> destino siguiendo los padres hacia atrás. O(longitud del camino). */
    public static List<Integer> reconstruir(Arbol arbol, int destino) {
        List<Integer> camino = new ArrayList<>();
        for (int v = destino; v != -1; v = arbol.padre()[v]) {
            camino.add(v);
        }
        Collections.reverse(camino);
        return camino;
    }
}
