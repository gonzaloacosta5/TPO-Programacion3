package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.soporte.GrafoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecorridosTest {

    @Test
    @DisplayName("BFS desde CABA visita por niveles en orden alfabético (verificado a mano)")
    void bfsDesdeCaba() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Recorridos.ResultadoBfs r = Recorridos.bfs(g, GrafoPrueba.i(g, "CABA"));

        assertEquals(List.of("CABA", "CAMPANA", "LOMAS_DE_ZAMORA", "PILAR", "QUILMES", "ROSARIO",
                "ZARATE", "LA_PLATA", "PARANA", "SANTA_FE", "CORRIENTES", "RESISTENCIA"), r.ordenVisita());
        assertEquals(12, r.nodosVisitados());
        assertEquals(0, r.nodosNoAlcanzados());

        // Saltos mínimos desde CABA
        assertEquals(0, r.saltosDesdeOrigen().get("CABA"));
        assertEquals(1, r.saltosDesdeOrigen().get("ROSARIO"));
        assertEquals(2, r.saltosDesdeOrigen().get("LA_PLATA"));
        assertEquals(2, r.saltosDesdeOrigen().get("SANTA_FE"));
        assertEquals(3, r.saltosDesdeOrigen().get("CORRIENTES"));
        assertEquals(3, r.saltosDesdeOrigen().get("RESISTENCIA"));

        // Árbol BFS
        assertEquals("CABA", r.padres().get("ROSARIO"));
        assertEquals("ROSARIO", r.padres().get("SANTA_FE"));
        assertEquals("PARANA", r.padres().get("CORRIENTES"));
        assertEquals("SANTA_FE", r.padres().get("RESISTENCIA"));
        assertEquals(11, r.padres().size(), "todos menos el origen tienen padre");
    }

    @Test
    @DisplayName("DFS desde CABA profundiza primero por el vecino de menor índice (verificado a mano)")
    void dfsDesdeCaba() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Recorridos.ResultadoDfs r = Recorridos.dfs(g, GrafoPrueba.i(g, "CABA"));

        assertEquals(List.of("CABA", "CAMPANA", "PILAR", "ZARATE", "ROSARIO", "PARANA", "CORRIENTES",
                "RESISTENCIA", "SANTA_FE", "LOMAS_DE_ZAMORA", "LA_PLATA", "QUILMES"), r.ordenVisita());
        assertEquals(List.of("PILAR", "SANTA_FE", "RESISTENCIA", "CORRIENTES", "PARANA", "ROSARIO", "ZARATE",
                "CAMPANA", "QUILMES", "LA_PLATA", "LOMAS_DE_ZAMORA", "CABA"), r.ordenFinalizacion());
        assertEquals(12, r.nodosVisitados());
        assertEquals("CAMPANA", r.padres().get("PILAR"));
        assertEquals("CORRIENTES", r.padres().get("RESISTENCIA"));
    }

    @Test
    @DisplayName("BFS y DFS informan los nodos que no se pueden alcanzar en un grafo desconectado")
    void grafoDesconectado() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        g.agregarVertice("AISLADO");
        g.agregarArista(a, b, 1);
        g.agregarArista(b, c, 1);
        g.ordenarVecinos();

        Recorridos.ResultadoBfs bfs = Recorridos.bfs(g, a);
        assertEquals(List.of("A", "B", "C"), bfs.ordenVisita());
        assertEquals(1, bfs.nodosNoAlcanzados());

        Recorridos.ResultadoDfs dfs = Recorridos.dfs(g, a);
        assertEquals(List.of("A", "B", "C"), dfs.ordenVisita());
        assertEquals(1, dfs.nodosNoAlcanzados());
    }

    @Test
    @DisplayName("Desde un nodo aislado solo se visita a sí mismo")
    void origenAislado() {
        GrafoMemoria g = new GrafoMemoria();
        int solo = g.agregarVertice("SOLO");
        g.agregarVertice("OTRO");
        assertEquals(List.of("SOLO"), Recorridos.bfs(g, solo).ordenVisita());
        assertEquals(List.of("SOLO"), Recorridos.dfs(g, solo).ordenVisita());
    }
}
