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

class DijkstraTest {

    @Test
    @DisplayName("CABA -> CORRIENTES = 747 min por Rosario, Santa Fe y Resistencia (cálculo a mano)")
    void cabaACorrientes() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Dijkstra.ResultadoCamino r = Dijkstra.caminoMinimo(g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES"));

        assertTrue(r.alcanzable());
        assertEquals(747L, r.costoMinutos());
        assertEquals(List.of("CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES"), r.camino());
        assertEquals(4, r.saltos());
        // 200 + 105 + 420 + 22 = 747
        assertEquals(List.of(
                new Dijkstra.Tramo("CABA", "ROSARIO", 200),
                new Dijkstra.Tramo("ROSARIO", "SANTA_FE", 105),
                new Dijkstra.Tramo("SANTA_FE", "RESISTENCIA", 420),
                new Dijkstra.Tramo("RESISTENCIA", "CORRIENTES", 22)), r.tramos());
    }

    @Test
    @DisplayName("QUILMES -> ZARATE rodea por CABA y Campana (140 min)")
    void quilmesAZarate() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Dijkstra.ResultadoCamino r = Dijkstra.caminoMinimo(g, GrafoPrueba.i(g, "QUILMES"), GrafoPrueba.i(g, "ZARATE"));
        assertEquals(140L, r.costoMinutos());
        assertEquals(List.of("QUILMES", "CABA", "CAMPANA", "ZARATE"), r.camino());
    }

    @Test
    @DisplayName("LA_PLATA -> PARANA = 418 min (cruza el conurbano y entra por Santa Fe)")
    void laPlataAParana() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Dijkstra.ResultadoCamino r = Dijkstra.caminoMinimo(g, GrafoPrueba.i(g, "LA_PLATA"), GrafoPrueba.i(g, "PARANA"));
        assertEquals(418L, r.costoMinutos());
        assertEquals(List.of("LA_PLATA", "QUILMES", "CABA", "ROSARIO", "SANTA_FE", "PARANA"), r.camino());
    }

    @Test
    @DisplayName("El camino de un nodo a sí mismo cuesta 0")
    void origenIgualDestino() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int caba = GrafoPrueba.i(g, "CABA");
        Dijkstra.ResultadoCamino r = Dijkstra.caminoMinimo(g, caba, caba);
        assertEquals(0L, r.costoMinutos());
        assertEquals(List.of("CABA"), r.camino());
        assertEquals(0, r.saltos());
    }

    @Test
    @DisplayName("Un destino inalcanzable se informa sin lanzar excepción")
    void inalcanzable() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        g.agregarArista(a, b, 3);
        Dijkstra.ResultadoCamino r = Dijkstra.caminoMinimo(g, a, c);
        assertFalse(r.alcanzable());
        assertNull(r.costoMinutos());
        assertTrue(r.camino().isEmpty());
    }

    @Test
    @DisplayName("Cortar al llegar al destino expande menos nodos que calcular todas las distancias")
    void cortaAlLlegarAlDestino() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int caba = GrafoPrueba.i(g, "CABA");
        Dijkstra.Arbol parcial = Dijkstra.calcular(g, caba, GrafoPrueba.i(g, "QUILMES"));
        Dijkstra.Arbol completo = Dijkstra.calcular(g, caba, -1);
        assertTrue(parcial.nodosExpandidos() < completo.nodosExpandidos());
        assertEquals(12, completo.nodosExpandidos());
    }

    @Test
    @DisplayName("Con aristas paralelas entre dos nodos usa la más barata")
    void aristasParalelas() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        g.agregarArista(a, b, 10);
        g.agregarArista(a, b, 4);
        assertEquals(4L, Dijkstra.caminoMinimo(g, a, b).costoMinutos());
    }

    @Test
    @DisplayName("Dijkstra rechaza pesos negativos")
    void pesosNegativos() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        g.agregarArista(a, b, -1);
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.caminoMinimo(g, a, b));
    }
}
