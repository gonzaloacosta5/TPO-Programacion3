package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.soporte.GrafoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloydWarshallTest {

    @Test
    @DisplayName("Costos de todos contra todos sobre la semilla (verificados contra un cálculo independiente)")
    void costosSobreLaSemilla() {
        GrafoMemoria g = GrafoPrueba.semilla();
        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);

        assertFalse(fw.tieneCicloNegativo());
        assertEquals(747L, fw.costo(GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES")));
        assertEquals(185L, fw.costo(GrafoPrueba.i(g, "LA_PLATA"), GrafoPrueba.i(g, "ZARATE")));
        assertEquals(665L, fw.costo(GrafoPrueba.i(g, "CAMPANA"), GrafoPrueba.i(g, "RESISTENCIA")));

        // Suma de las distancias de los 66 pares no ordenados = 21058 (calculada fuera de este código)
        long suma = 0;
        for (int i = 0; i < g.numVertices(); i++) {
            assertEquals(0L, fw.costo(i, i));
            for (int j = i + 1; j < g.numVertices(); j++) {
                assertEquals(fw.costo(i, j), fw.costo(j, i), "la red es no dirigida: la matriz es simétrica");
                suma += fw.costo(i, j);
            }
        }
        assertEquals(21058L, suma);
    }

    @Test
    @DisplayName("Floyd-Warshall coincide con correr Dijkstra desde cada vértice")
    void coincideConDijkstraVVeces() {
        GrafoMemoria g = GrafoPrueba.semilla();
        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);
        for (int origen = 0; origen < g.numVertices(); origen++) {
            Dijkstra.Arbol arbol = Dijkstra.calcular(g, origen, -1);
            for (int destino = 0; destino < g.numVertices(); destino++) {
                assertEquals(arbol.distancia()[destino], fw.costo(origen, destino),
                        g.id(origen) + " -> " + g.id(destino));
            }
        }
    }

    @Test
    @DisplayName("La matriz de siguientes permite reconstruir el camino mínimo")
    void reconstruccionDeCaminos() {
        GrafoMemoria g = GrafoPrueba.semilla();
        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);

        assertEquals(List.of("CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES"),
                fw.caminoIds(GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES")));
        assertEquals(List.of("QUILMES", "CABA", "CAMPANA", "ZARATE"),
                fw.caminoIds(GrafoPrueba.i(g, "QUILMES"), GrafoPrueba.i(g, "ZARATE")));
        assertEquals(List.of("PILAR"), fw.caminoIds(GrafoPrueba.i(g, "PILAR"), GrafoPrueba.i(g, "PILAR")));
    }

    @Test
    @DisplayName("El resultado serializable trae las matrices, el conteo V³ y ninguna celda en null en un grafo conexo")
    void resultadoSerializable() {
        GrafoMemoria g = GrafoPrueba.semilla();
        FloydWarshall.Resultado r = FloydWarshall.ejecutar(g);

        assertEquals(12, r.nodos().size());
        assertEquals(12, r.matrizCostos().size());
        assertEquals(12, r.matrizSiguiente().get(0).size());
        assertEquals(1728L, r.iteracionesInternas(), "12³ iteraciones del triple bucle");
        assertFalse(r.tieneCicloNegativo());
        assertTrue(r.nodosEnCicloNegativo().isEmpty());
        assertEquals(0L, r.matrizCostos().get(0).get(0));
        assertEquals("CABA", r.matrizSiguiente().get(0).get(0));
    }

    @Test
    @DisplayName("Detecta un ciclo negativo A -> B -> C -> A (2 - 5 + 1 = -2) e identifica sus nodos")
    void detectaCicloNegativo() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        int d = g.agregarVertice("D");
        g.agregarArco(a, b, 2);
        g.agregarArco(b, c, -5);
        g.agregarArco(c, a, 1);
        g.agregarArco(d, a, 4);

        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);
        assertTrue(fw.tieneCicloNegativo());
        FloydWarshall.Resultado r = fw.aResultado();
        assertTrue(r.tieneCicloNegativo());
        assertEquals(List.of("A", "B", "C"), r.nodosEnCicloNegativo(), "D llega al ciclo pero no forma parte de él");
        assertNull(r.matrizCostos(), "con ciclo negativo las matrices no tienen significado");
        assertThrows(IllegalStateException.class, () -> fw.camino(a, c));
    }

    @Test
    @DisplayName("Una arista negativa SIN ciclo es válida: Floyd-Warshall la maneja (Dijkstra no)")
    void aristaNegativaSinCiclo() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        g.agregarArco(a, b, -2);
        g.agregarArco(b, c, 3);
        g.agregarArco(a, c, 5);

        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);
        assertFalse(fw.tieneCicloNegativo());
        assertEquals(1L, fw.costo(a, c));
        assertEquals(List.of("A", "B", "C"), fw.caminoIds(a, c));
        assertEquals(GrafoMemoria.INFINITO, fw.costo(c, a), "en un grafo dirigido no hay vuelta");
        assertTrue(fw.camino(c, a).isEmpty());
        assertNull(fw.aResultado().matrizCostos().get(2).get(0), "inalcanzable se representa con null");
    }

    @Test
    @DisplayName("Una arista no dirigida negativa implica un ciclo negativo (ida y vuelta)")
    void aristaNoDirigidaNegativa() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        g.agregarArista(a, b, -1);
        assertTrue(FloydWarshall.calcular(g).tieneCicloNegativo());
    }

    @Test
    @DisplayName("Un bucle de costo negativo sobre un nodo también es un ciclo negativo")
    void bucleNegativo() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        g.agregarArco(a, a, -3);
        assertTrue(FloydWarshall.calcular(g).tieneCicloNegativo());
    }
}
