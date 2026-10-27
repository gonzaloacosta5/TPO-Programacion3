package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.algorithm.estructura.UnionFind;
import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Kruskal: ordena todas las aristas por peso (MergeSort propio) y las va agregando si no
 * cierran un ciclo, lo que se decide con {@link UnionFind}.
 *
 * <p><b>Complejidad:</b> ordenar E aristas O(E log E) = O(E log V); recorrerlas con Union-Find
 * O(E α(V)), prácticamente lineal. Total O(E log V). Espacio O(V + E).</p>
 * Si el grafo no es conexo devuelve un bosque de expansión mínima.
 */
public final class Kruskal {

    private Kruskal() {
    }

    public static ResultadoMst ejecutar(GrafoMemoria g) {
        if (g.tieneArcosDirigidos()) {
            throw new IllegalArgumentException("Kruskal requiere un grafo no dirigido");
        }
        int n = g.numVertices();
        GrafoMemoria.AristaGlobal[] aristas = g.aristas().toArray(new GrafoMemoria.AristaGlobal[0]);
        Ordenamiento.mergeSort(aristas, Comparator
                .comparingInt(GrafoMemoria.AristaGlobal::peso)
                .thenComparingInt(GrafoMemoria.AristaGlobal::origen)
                .thenComparingInt(GrafoMemoria.AristaGlobal::destino));

        UnionFind uf = new UnionFind(n);
        List<ResultadoMst.AristaMst> elegidas = new ArrayList<>();
        long total = 0;
        for (GrafoMemoria.AristaGlobal a : aristas) {
            if (elegidas.size() == n - 1) {
                break; // el árbol ya está completo
            }
            if (uf.unir(a.origen(), a.destino())) {
                elegidas.add(new ResultadoMst.AristaMst(g.id(a.origen()), g.id(a.destino()), a.peso()));
                total += a.peso();
            }
        }
        return new ResultadoMst("Kruskal", null, total, elegidas.size(), n, uf.componentes() <= 1, elegidas);
    }
}
