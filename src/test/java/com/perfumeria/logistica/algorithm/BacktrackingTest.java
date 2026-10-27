package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.soporte.GrafoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacktrackingTest {

    @Test
    @DisplayName("CABA -> QUILMES con tope 100 min y 3 saltos: 2 rutas; la poda explora 9 nodos contra 60 sin poda")
    void podaReduceElArbolDeBusqueda() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Backtracking.Resultado r = Backtracking.buscarRutas(
                g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "QUILMES"), 100, 3, 50);

        assertEquals(2, r.totalRutasValidas());
        assertEquals(List.of("CABA", "QUILMES"), r.rutas().get(0).camino());
        assertEquals(40L, r.rutas().get(0).minutos());
        assertEquals(List.of("CABA", "LOMAS_DE_ZAMORA", "QUILMES"), r.rutas().get(1).camino());
        assertEquals(57L, r.rutas().get(1).minutos());
        assertEquals(r.rutas().get(0), r.mejorRuta());

        // Conteos verificados con una reimplementación independiente en Python
        assertEquals(9L, r.nodosExploradosConPoda());
        assertEquals(60L, r.nodosExploradosSinPoda());
        assertEquals(51L, r.nodosEvitadosPorPoda());
        assertEquals(85.0, r.reduccionPorcentual());
    }

    @Test
    @DisplayName("CABA -> CORRIENTES con 800 min y 6 saltos: 4 rutas válidas ordenadas por tiempo")
    void rutasHaciaElNea() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Backtracking.Resultado r = Backtracking.buscarRutas(
                g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES"), 800, 6, 50);

        assertEquals(4, r.totalRutasValidas());
        assertEquals(List.of(747L, 767L, 783L, 800L), r.rutas().stream().map(Backtracking.RutaValida::minutos).toList());
        assertEquals(List.of(4, 6, 4, 3), r.rutas().stream().map(Backtracking.RutaValida::saltos).toList());
        assertEquals(List.of("CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES"), r.mejorRuta().camino());
        assertEquals(43L, r.nodosExploradosConPoda());
        assertEquals(53L, r.nodosExploradosSinPoda());
    }

    @Test
    @DisplayName("Con la poda por saltos (5) y por costo (760) queda una sola ruta")
    void restriccionesAjustadas() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Backtracking.Resultado r = Backtracking.buscarRutas(
                g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES"), 760, 5, 50);
        assertEquals(1, r.totalRutasValidas());
        assertEquals(747L, r.mejorRuta().minutos());
        assertEquals(36L, r.nodosExploradosConPoda());
        assertEquals(53L, r.nodosExploradosSinPoda());
    }

    @Test
    @DisplayName("Con topes holgados la poda no puede evitar nada: ambas variantes exploran lo mismo")
    void sinRestriccionesLaPodaNoAyuda() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Backtracking.Resultado r = Backtracking.buscarRutas(
                g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES"), 5000, 11, 100);
        assertEquals(12, r.totalRutasValidas(), "hay 12 rutas simples de CABA a CORRIENTES");
        assertEquals(53L, r.nodosExploradosConPoda());
        assertEquals(53L, r.nodosExploradosSinPoda());
        assertEquals(0L, r.nodosEvitadosPorPoda());
    }

    @Test
    @DisplayName("Con y sin poda se encuentran exactamente las mismas rutas, y todas respetan los topes")
    void mismasRutasConYSinPoda() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int origen = GrafoPrueba.i(g, "LA_PLATA");
        int destino = GrafoPrueba.i(g, "RESISTENCIA");

        Backtracking.Ejecucion con = Backtracking.ejecutar(g, origen, destino, 900, 8, true);
        Backtracking.Ejecucion sin = Backtracking.ejecutar(g, origen, destino, 900, 8, false);

        assertEquals(15, con.rutas().size());
        assertEquals(con.rutas(), sin.rutas());
        assertEquals(125L, con.nodosExplorados());
        assertEquals(179L, sin.nodosExplorados());
        for (Backtracking.RutaValida ruta : con.rutas()) {
            assertTrue(ruta.minutos() <= 900);
            assertTrue(ruta.saltos() <= 8);
            assertEquals(ruta.saltos() + 1, ruta.camino().size());
            assertEquals("LA_PLATA", ruta.camino().get(0));
            assertEquals("RESISTENCIA", ruta.camino().get(ruta.camino().size() - 1));
            assertEquals(ruta.camino().size(), ruta.camino().stream().distinct().count(), "ruta simple: sin nodos repetidos");
        }
    }

    @Test
    @DisplayName("Si ninguna ruta cumple los topes devuelve cero rutas y mejorRuta nula")
    void sinSolucion() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Backtracking.Resultado r = Backtracking.buscarRutas(
                g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES"), 100, 3, 10);
        assertEquals(0, r.totalRutasValidas());
        assertNull(r.mejorRuta());
        assertTrue(r.rutas().isEmpty());
    }

    @Test
    @DisplayName("El límite de rutas devueltas recorta la lista pero no el total informado")
    void limiteDeRutasDevueltas() {
        GrafoMemoria g = GrafoPrueba.semilla();
        Backtracking.Resultado r = Backtracking.buscarRutas(
                g, GrafoPrueba.i(g, "CABA"), GrafoPrueba.i(g, "CORRIENTES"), 5000, 11, 3);
        assertEquals(12, r.totalRutasValidas());
        assertEquals(3, r.rutas().size());
        assertEquals(747L, r.rutas().get(0).minutos());
    }

    @Test
    @DisplayName("Rechaza topes negativos y pesos negativos")
    void entradasInvalidas() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int a = GrafoPrueba.i(g, "CABA");
        int b = GrafoPrueba.i(g, "QUILMES");
        assertThrows(IllegalArgumentException.class, () -> Backtracking.buscarRutas(g, a, b, -1, 3, 5));
        assertThrows(IllegalArgumentException.class, () -> Backtracking.buscarRutas(g, a, b, 100, -1, 5));

        GrafoMemoria negativo = new GrafoMemoria();
        int x = negativo.agregarVertice("X");
        int y = negativo.agregarVertice("Y");
        negativo.agregarArista(x, y, -4);
        assertThrows(IllegalArgumentException.class, () -> Backtracking.buscarRutas(negativo, x, y, 10, 2, 5));
    }
}
