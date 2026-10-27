package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.algorithm.estructura.MinHeap;

import java.util.ArrayList;
import java.util.List;

/**
 * Prim con min-heap propio: hace crecer un único árbol desde el origen sumando siempre la
 * arista más barata que sale del árbol.
 *
 * <p><b>Complejidad:</b> cada arco se examina una vez y puede insertar una entrada en el heap
 * (hasta O(E) entradas, cada operación O(log E) = O(log V)). Total O(E log V).
 * Espacio O(V + E) (arreglos de tamaño V + heap con entradas obsoletas).</p>
 * Con búsqueda lineal del mínimo sería O(V²): por eso se usa el heap.
 */
public final class Prim {

    private Prim() {
    }

    public static ResultadoMst ejecutar(GrafoMemoria g, int origen) {
        if (g.tieneArcosDirigidos()) {
            throw new IllegalArgumentException("Prim requiere un grafo no dirigido");
        }
        int n = g.numVertices();
        long[] clave = new long[n];      // mejor peso conocido para conectar cada vértice al árbol
        int[] padre = new int[n];        // extremo del árbol con el que se conecta
        boolean[] enArbol = new boolean[n];
        for (int i = 0; i < n; i++) {
            clave[i] = GrafoMemoria.INFINITO;
            padre[i] = -1;
        }

        MinHeap heap = new MinHeap();
        clave[origen] = 0;
        heap.insertar(0, origen);

        List<ResultadoMst.AristaMst> elegidas = new ArrayList<>();
        long total = 0;
        int cubiertos = 0;
        while (!heap.estaVacio()) {
            int u = heap.extraerMinimo().valor();
            if (enArbol[u]) {
                continue; // entrada obsoleta
            }
            enArbol[u] = true;
            cubiertos++;
            if (padre[u] >= 0) {
                elegidas.add(new ResultadoMst.AristaMst(g.id(padre[u]), g.id(u), (int) clave[u]));
                total += clave[u];
            }
            for (GrafoMemoria.Arista a : g.vecinos(u)) {
                int v = a.destino();
                if (!enArbol[v] && a.peso() < clave[v]) {
                    clave[v] = a.peso();
                    padre[v] = u;
                    heap.insertar(a.peso(), v);
                }
            }
        }
        return new ResultadoMst("Prim", g.id(origen), total, elegidas.size(), cubiertos, cubiertos == n, elegidas);
    }
}
