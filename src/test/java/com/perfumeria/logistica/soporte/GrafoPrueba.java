package com.perfumeria.logistica.soporte;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.repository.DatosSemilla;

/** Utilidades compartidas por los tests de algoritmos. */
public final class GrafoPrueba {

    private GrafoPrueba() {
    }

    /** El grafo de la red logística de ejemplo (12 nodos, 17 rutas), armado en memoria. */
    public static GrafoMemoria semilla() {
        return GrafoMemoria.desde(DatosSemilla.nodos(), DatosSemilla.rutas());
    }

    /** Índice de un nodo; falla el test con un mensaje claro si el id no existe. */
    public static int i(GrafoMemoria g, String id) {
        int indice = g.indice(id);
        if (indice < 0) {
            throw new IllegalArgumentException("El nodo de prueba no existe: " + id);
        }
        return indice;
    }
}
