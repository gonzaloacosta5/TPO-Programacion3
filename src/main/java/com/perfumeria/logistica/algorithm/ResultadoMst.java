package com.perfumeria.logistica.algorithm;

import java.util.List;

/**
 * Resultado común de Prim y Kruskal.
 *
 * @param origen         vértice inicial de Prim (null en Kruskal, que no tiene origen)
 * @param conexo         true si el árbol cubre todos los vértices del grafo; si es false,
 *                       Prim cubrió solo la componente del origen y Kruskal devolvió un bosque
 * @param nodosCubiertos vértices alcanzados por las aristas elegidas (Prim) o total de vértices (Kruskal)
 */
public record ResultadoMst(String algoritmo, String origen, long costoTotalMinutos, int aristasEnArbol,
                           int nodosCubiertos, boolean conexo, List<AristaMst> aristas) {

    public record AristaMst(String desde, String hasta, int minutos) {
    }
}
