package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.soporte.GrafoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArbolExpansionMinimaTest {

    /**
     * Árbol de expansión mínimo calculado a mano con Kruskal sobre la semilla (los pesos son todos distintos,
     * así que es único): 20+22+25+28+32+35+45+55+105+120+420 = 907 minutos, 11 aristas.
     */
    private static final Set<String> MST_ESPERADO = Set.of(
            "CAMPANA-ZARATE:20",
            "CORRIENTES-RESISTENCIA:22",
            "LOMAS_DE_ZAMORA-QUILMES:25",
            "PARANA-SANTA_FE:28",
            "CABA-LOMAS_DE_ZAMORA:32",
            "CAMPANA-PILAR:35",
            "LA_PLATA-QUILMES:45",
            "CABA-PILAR:55",
            "ROSARIO-SANTA_FE:105",
            "ROSARIO-ZARATE:120",
            "RESISTENCIA-SANTA_FE:420");

    @Test
    @DisplayName("Kruskal (con Union-Find) obtiene el MST de 907 minutos calculado a mano")
    void kruskalSobreLaSemilla() {
        ResultadoMst r = Kruskal.ejecutar(GrafoPrueba.semilla());
        assertEquals(907L, r.costoTotalMinutos());
        assertEquals(11, r.aristasEnArbol());
        assertTrue(r.conexo());
        assertEquals(MST_ESPERADO, comoConjunto(r));
    }

    @Test
    @DisplayName("Prim obtiene el mismo MST desde cualquier nodo de origen")
    void primDesdeCualquierOrigen() {
        GrafoMemoria g = GrafoPrueba.semilla();
        for (int origen = 0; origen < g.numVertices(); origen++) {
            ResultadoMst r = Prim.ejecutar(g, origen);
            assertEquals(907L, r.costoTotalMinutos(), "origen " + g.id(origen));
            assertEquals(11, r.aristasEnArbol());
            assertTrue(r.conexo());
            assertEquals(MST_ESPERADO, comoConjunto(r), "origen " + g.id(origen));
        }
    }

    @Test
    @DisplayName("Prim y Kruskal coinciden en costo con pesos repetidos (el árbol puede diferir)")
    void pesosRepetidos() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        int d = g.agregarVertice("D");
        g.agregarArista(a, b, 1);
        g.agregarArista(b, c, 1);
        g.agregarArista(c, d, 1);
        g.agregarArista(a, d, 1);
        g.agregarArista(a, c, 1);
        g.ordenarVecinos();
        assertEquals(3L, Prim.ejecutar(g, a).costoTotalMinutos());
        assertEquals(3L, Kruskal.ejecutar(g).costoTotalMinutos());
    }

    @Test
    @DisplayName("En un grafo desconectado Kruskal devuelve un bosque y Prim solo cubre la componente del origen")
    void grafoDesconectado() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        int d = g.agregarVertice("D");
        g.agregarArista(a, b, 5);
        g.agregarArista(c, d, 7);
        g.ordenarVecinos();

        ResultadoMst kruskal = Kruskal.ejecutar(g);
        assertFalse(kruskal.conexo());
        assertEquals(2, kruskal.aristasEnArbol());
        assertEquals(12L, kruskal.costoTotalMinutos());

        ResultadoMst prim = Prim.ejecutar(g, a);
        assertFalse(prim.conexo());
        assertEquals(2, prim.nodosCubiertos());
        assertEquals(5L, prim.costoTotalMinutos());
    }

    @Test
    @DisplayName("Con arcos dirigidos no se define el MST")
    void grafoDirigido() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        g.agregarArco(a, b, 1);
        assertThrows(IllegalArgumentException.class, () -> Prim.ejecutar(g, a));
        assertThrows(IllegalArgumentException.class, () -> Kruskal.ejecutar(g));
    }

    /** Normaliza cada arista como "MENOR-MAYOR:peso" para comparar sin importar el sentido. */
    private static Set<String> comoConjunto(ResultadoMst r) {
        Set<String> conjunto = new HashSet<>();
        for (ResultadoMst.AristaMst a : r.aristas()) {
            String desde = a.desde().compareTo(a.hasta()) <= 0 ? a.desde() : a.hasta();
            String hasta = a.desde().compareTo(a.hasta()) <= 0 ? a.hasta() : a.desde();
            conjunto.add(desde + "-" + hasta + ":" + a.minutos());
        }
        assertEquals(r.aristas().size(), conjunto.size(), "no debe haber aristas repetidas");
        return conjunto;
    }
}
