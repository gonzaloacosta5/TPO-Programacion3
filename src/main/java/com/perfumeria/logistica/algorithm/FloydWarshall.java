package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;

import java.util.ArrayList;
import java.util.List;

/**
 * Floyd-Warshall: costo mínimo entre TODOS los pares de vértices, con detección de ciclos negativos
 * y reconstrucción de caminos.
 *
 * <p>Programación dinámica: {@code d_k[i][j]} es el mejor costo de i a j usando solo los vértices
 * 0..k-1 como intermedios, y
 * {@code d_k+1[i][j] = min(d_k[i][j], d_k[i][k] + d_k[k][j])}. Se actualiza en el lugar.
 * La matriz {@code siguiente[i][j]} guarda el primer salto del camino de i a j, lo que permite
 * reconstruir cualquier camino sin volver a calcular.</p>
 *
 * <p><b>Ciclo negativo:</b> si al terminar algún {@code d[i][i] < 0}, existe un ciclo de costo negativo
 * que pasa por i y los costos mínimos dejan de estar definidos. Se informa en lugar de devolver
 * números sin sentido.</p>
 *
 * <p><b>Complejidad:</b> Θ(V³) tiempo (tres bucles anidados de V iteraciones), Θ(V²) espacio
 * (matrices de costos y de siguientes).</p>
 */
public final class FloydWarshall {

    private FloydWarshall() {
    }

    /**
     * Vista serializable del resultado. Los costos inalcanzables se representan con {@code null}.
     * Si hay ciclo negativo las matrices se omiten (null) porque no tienen significado.
     */
    public record Resultado(List<String> nodos, boolean tieneCicloNegativo, List<String> nodosEnCicloNegativo,
                            List<List<Long>> matrizCostos, List<List<String>> matrizSiguiente,
                            long iteracionesInternas, long actualizaciones) {
    }

    /** Estado completo del cálculo, con el que se pueden pedir costos y caminos puntuales. */
    public static final class Matrices {
        private final GrafoMemoria grafo;
        private final long[][] dist;
        private final int[][] siguiente;
        private final List<Integer> enCicloNegativo;
        private final long iteraciones;
        private final long actualizaciones;

        private Matrices(GrafoMemoria grafo, long[][] dist, int[][] siguiente, List<Integer> enCicloNegativo,
                         long iteraciones, long actualizaciones) {
            this.grafo = grafo;
            this.dist = dist;
            this.siguiente = siguiente;
            this.enCicloNegativo = enCicloNegativo;
            this.iteraciones = iteraciones;
            this.actualizaciones = actualizaciones;
        }

        public boolean tieneCicloNegativo() {
            return !enCicloNegativo.isEmpty();
        }

        public long iteracionesInternas() {
            return iteraciones;
        }

        public long actualizaciones() {
            return actualizaciones;
        }

        /** Costo mínimo de i a j, o {@link GrafoMemoria#INFINITO} si no hay camino. */
        public long costo(int i, int j) {
            return dist[i][j];
        }

        /** Camino mínimo de i a j como índices de vértice; vacío si no hay camino. O(longitud). */
        public List<Integer> camino(int i, int j) {
            if (tieneCicloNegativo()) {
                throw new IllegalStateException("Con ciclos negativos los caminos mínimos no están definidos");
            }
            List<Integer> camino = new ArrayList<>();
            if (dist[i][j] >= GrafoMemoria.INFINITO) {
                return camino;
            }
            int actual = i;
            camino.add(actual);
            while (actual != j) {
                actual = siguiente[actual][j];
                camino.add(actual);
            }
            return camino;
        }

        /** Igual que {@link #camino(int, int)} pero con los ids de los nodos. */
        public List<String> caminoIds(int i, int j) {
            List<String> ids = new ArrayList<>();
            for (int v : camino(i, j)) {
                ids.add(grafo.id(v));
            }
            return ids;
        }

        public Resultado aResultado() {
            int n = grafo.numVertices();
            List<String> ids = new ArrayList<>(grafo.ids());
            if (tieneCicloNegativo()) {
                List<String> afectados = new ArrayList<>();
                for (int v : enCicloNegativo) {
                    afectados.add(grafo.id(v));
                }
                return new Resultado(ids, true, afectados, null, null, iteraciones, actualizaciones);
            }
            List<List<Long>> costos = new ArrayList<>();
            List<List<String>> siguientes = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                List<Long> filaCostos = new ArrayList<>();
                List<String> filaSiguientes = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    filaCostos.add(dist[i][j] >= GrafoMemoria.INFINITO ? null : dist[i][j]);
                    filaSiguientes.add(siguiente[i][j] < 0 ? null : grafo.id(siguiente[i][j]));
                }
                costos.add(filaCostos);
                siguientes.add(filaSiguientes);
            }
            return new Resultado(ids, false, List.of(), costos, siguientes, iteraciones, actualizaciones);
        }
    }

    public static Matrices calcular(GrafoMemoria g) {
        int n = g.numVertices();
        long inf = GrafoMemoria.INFINITO;
        long[][] d = new long[n][n];
        int[][] sig = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = inf;
                sig[i][j] = -1;
            }
            d[i][i] = 0;
            sig[i][i] = i;
        }
        for (int u = 0; u < n; u++) {
            for (GrafoMemoria.Arista a : g.vecinos(u)) {
                if (a.peso() < d[u][a.destino()]) { // en paralelas se queda con la más barata
                    d[u][a.destino()] = a.peso();
                    sig[u][a.destino()] = a.destino();
                }
            }
        }

        long iteraciones = 0;
        long actualizaciones = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    iteraciones++;
                    if (d[i][k] >= inf || d[k][j] >= inf) {
                        continue; // no hay camino i->k o k->j: pasar por k no mejora nada
                    }
                    long candidata = d[i][k] + d[k][j];
                    if (candidata < -inf) {
                        candidata = -inf; // evita desbordes cuando hay ciclos negativos
                    }
                    if (candidata < d[i][j]) {
                        d[i][j] = candidata;
                        sig[i][j] = sig[i][k];
                        actualizaciones++;
                    }
                }
            }
        }

        List<Integer> enCiclo = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (d[i][i] < 0) {
                enCiclo.add(i);
            }
        }
        return new Matrices(g, d, sig, enCiclo, iteraciones, actualizaciones);
    }

    public static Resultado ejecutar(GrafoMemoria g) {
        return calcular(g).aResultado();
    }
}
