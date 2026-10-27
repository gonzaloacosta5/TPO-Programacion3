package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Backtracking: todas las rutas SIMPLES (sin repetir nodos) de entrega entre un origen y un destino que
 * respeten un tope de costo en minutos y un tope de saltos (aristas recorridas = transbordos + 1).
 *
 * <p>Se ejecuta dos veces sobre el mismo problema para poder comparar:
 * <ul>
 *   <li><b>Con poda:</b> antes de entrar a un vecino se verifica si el costo o los saltos acumulados ya
 *       exceden los topes; si es así esa rama completa se descarta sin generarla.</li>
 *   <li><b>Sin poda:</b> se explora todo el árbol de rutas simples y las restricciones se chequean recién
 *       al llegar al destino.</li>
 * </ul>
 * Ambas variantes devuelven exactamente las mismas rutas; cambia cuántos nodos del árbol de búsqueda
 * hubo que explorar. Como los pesos no son negativos, el costo acumulado nunca baja y la poda es válida.</p>
 *
 * <p><b>Complejidad:</b> el árbol de rutas simples tiene hasta O(V!) nodos en el peor caso (grafo
 * completo), así que el tiempo es factorial con o sin poda; la poda reduce la constante, no el orden.
 * Espacio O(V): pila de recursión + arreglo de nodos en ruta.</p>
 */
public final class Backtracking {

    /** Tope de nodos explorados por cada ejecución (con o sin poda). */
    public static final long LIMITE_NODOS = 20_000_000L;

    private Backtracking() {
    }

    public record RutaValida(List<String> camino, long minutos, int saltos) {
    }

    public record Resultado(String origen, String destino, long maxMinutos, int maxSaltos,
                            int totalRutasValidas, RutaValida mejorRuta, List<RutaValida> rutas,
                            long nodosExploradosConPoda, long nodosExploradosSinPoda,
                            long nodosEvitadosPorPoda, double reduccionPorcentual) {
    }

    /** Resultado de una sola ejecución (con o sin poda). */
    public record Ejecucion(List<RutaValida> rutas, long nodosExplorados) {
    }

    /**
     * @param limiteRutasDevueltas cuántas rutas (las más rápidas) incluir en la respuesta; el total se
     *                             informa igual en {@code totalRutasValidas}
     */
    public static Resultado buscarRutas(GrafoMemoria g, int origen, int destino, long maxMinutos, int maxSaltos,
                                        int limiteRutasDevueltas) {
        if (g.tienePesosNegativos()) {
            throw new IllegalArgumentException("El backtracking con poda por costo requiere pesos no negativos");
        }
        if (maxMinutos < 0 || maxSaltos < 0) {
            throw new IllegalArgumentException("Los topes de minutos y de saltos no pueden ser negativos");
        }

        Ejecucion conPoda = ejecutar(g, origen, destino, maxMinutos, maxSaltos, true);
        Ejecucion sinPoda = ejecutar(g, origen, destino, maxMinutos, maxSaltos, false);

        // Mejores rutas primero: por minutos, luego por saltos (MergeSort estable, propio).
        RutaValida[] ordenadas = conPoda.rutas().toArray(new RutaValida[0]);
        Ordenamiento.mergeSort(ordenadas, Comparator
                .comparingLong(RutaValida::minutos)
                .thenComparingInt(RutaValida::saltos));

        List<RutaValida> devueltas = new ArrayList<>();
        for (int i = 0; i < ordenadas.length && i < limiteRutasDevueltas; i++) {
            devueltas.add(ordenadas[i]);
        }
        long evitados = sinPoda.nodosExplorados() - conPoda.nodosExplorados();
        double reduccion = sinPoda.nodosExplorados() == 0 ? 0.0
                : Math.round(10000.0 * evitados / sinPoda.nodosExplorados()) / 100.0;

        return new Resultado(g.id(origen), g.id(destino), maxMinutos, maxSaltos, ordenadas.length,
                ordenadas.length == 0 ? null : ordenadas[0], devueltas,
                conPoda.nodosExplorados(), sinPoda.nodosExplorados(), evitados, reduccion);
    }

    /** Una ejecución del backtracking. Público para poder testear cada variante por separado. */
    public static Ejecucion ejecutar(GrafoMemoria g, int origen, int destino, long maxMinutos, int maxSaltos,
                                     boolean usarPoda) {
        Busqueda b = new Busqueda(g, destino, maxMinutos, maxSaltos, usarPoda);
        b.enRuta[origen] = true;
        b.ruta[0] = origen;
        b.explorar(origen, 0L, 0);
        return new Ejecucion(b.encontradas, b.explorados);
    }

    private static final class Busqueda {
        final GrafoMemoria g;
        final int destino;
        final long maxMinutos;
        final int maxSaltos;
        final boolean poda;
        final boolean[] enRuta;
        final int[] ruta;
        int largo = 1; // cantidad de nodos actualmente en la ruta
        long explorados;
        final List<RutaValida> encontradas = new ArrayList<>();

        Busqueda(GrafoMemoria g, int destino, long maxMinutos, int maxSaltos, boolean poda) {
            this.g = g;
            this.destino = destino;
            this.maxMinutos = maxMinutos;
            this.maxSaltos = maxSaltos;
            this.poda = poda;
            this.enRuta = new boolean[g.numVertices()];
            this.ruta = new int[g.numVertices()];
        }

        void explorar(int u, long costo, int saltos) {
            explorados++;
            if (explorados > LIMITE_NODOS) {
                throw new LimiteComputoExcedidoException("El backtracking superó el límite de "
                        + LIMITE_NODOS + " nodos explorados; achicá los topes o elegí nodos más cercanos");
            }
            if (u == destino) {
                if (costo <= maxMinutos && saltos <= maxSaltos) {
                    registrar(costo, saltos);
                }
                return; // una ruta simple termina al llegar al destino
            }
            for (GrafoMemoria.Arista a : g.vecinos(u)) {
                int v = a.destino();
                if (enRuta[v]) {
                    continue; // no se repiten nodos
                }
                long nuevoCosto = costo + a.peso();
                int nuevosSaltos = saltos + 1;
                if (poda && (nuevoCosto > maxMinutos || nuevosSaltos > maxSaltos)) {
                    continue; // PODA: esta rama ya no puede cumplir las restricciones
                }
                enRuta[v] = true;
                ruta[largo++] = v;
                explorar(v, nuevoCosto, nuevosSaltos);
                largo--;
                enRuta[v] = false; // backtrack
            }
        }

        private void registrar(long costo, int saltos) {
            List<String> camino = new ArrayList<>();
            for (int i = 0; i < largo; i++) {
                camino.add(g.id(ruta[i]));
            }
            encontradas.add(new RutaValida(camino, costo, saltos));
        }
    }
}
