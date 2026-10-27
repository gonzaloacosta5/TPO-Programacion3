package com.perfumeria.logistica.algorithm.estructura;

/**
 * Union-Find (conjuntos disjuntos) con compresión de caminos (path halving) y unión por rango.
 * Cada operación cuesta O(α(n)) amortizado, donde α es la inversa de Ackermann (&le; 4 en la práctica).
 * Es la estructura que hace que Kruskal gaste O(E log E) en ordenar y casi nada en detectar ciclos.
 */
public final class UnionFind {

    private final int[] padre;
    private final int[] rango;
    private int componentes;

    public UnionFind(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n no puede ser negativo");
        }
        padre = new int[n];
        rango = new int[n];
        componentes = n;
        for (int i = 0; i < n; i++) {
            padre[i] = i;
        }
    }

    /** Representante del conjunto de x. */
    public int buscar(int x) {
        while (padre[x] != x) {
            padre[x] = padre[padre[x]]; // path halving
            x = padre[x];
        }
        return x;
    }

    /** Une los conjuntos de a y b. Devuelve false si ya estaban unidos (la arista cerraría un ciclo). */
    public boolean unir(int a, int b) {
        int ra = buscar(a);
        int rb = buscar(b);
        if (ra == rb) {
            return false;
        }
        if (rango[ra] < rango[rb]) {
            padre[ra] = rb;
        } else if (rango[ra] > rango[rb]) {
            padre[rb] = ra;
        } else {
            padre[rb] = ra;
            rango[ra]++;
        }
        componentes--;
        return true;
    }

    public boolean conectados(int a, int b) {
        return buscar(a) == buscar(b);
    }

    /** Cantidad de conjuntos disjuntos que quedan. */
    public int componentes() {
        return componentes;
    }
}
