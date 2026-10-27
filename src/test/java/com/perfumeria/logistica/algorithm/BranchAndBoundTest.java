package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.soporte.GrafoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchAndBoundTest {

    private static int[] indices(GrafoMemoria g, String... ids) {
        int[] r = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            r[i] = GrafoPrueba.i(g, ids[i]);
        }
        return r;
    }

    @Test
    @DisplayName("Reparto CABA -> Quilmes, La Plata, Pilar -> CABA: 280 min (óptimo verificado por fuerza bruta externa)")
    void repartoCorto() {
        GrafoMemoria g = GrafoPrueba.semilla();
        BranchAndBound.Resultado r = BranchAndBound.optimizarReparto(
                g, GrafoPrueba.i(g, "CABA"), indices(g, "QUILMES", "LA_PLATA", "PILAR"), true);

        assertEquals(280L, r.costoTotalMinutos());
        assertEquals("CABA", r.ordenDeEntrega().get(0));
        assertEquals("CABA", r.ordenDeEntrega().get(r.ordenDeEntrega().size() - 1));
        assertEquals(5, r.ordenDeEntrega().size());
        assertTrue(r.cotaActivada());
        assertEquals(280L, costoDeRuta(g, r.rutaFisicaCompleta()));
    }

    @Test
    @DisplayName("Reparto de 6 destinos del GBA y la zona norte: 387 min")
    void repartoMedio() {
        GrafoMemoria g = GrafoPrueba.semilla();
        BranchAndBound.Resultado r = BranchAndBound.optimizarReparto(g, GrafoPrueba.i(g, "CABA"),
                indices(g, "QUILMES", "LA_PLATA", "PILAR", "CAMPANA", "ZARATE", "LOMAS_DE_ZAMORA"), true);
        assertEquals(387L, r.costoTotalMinutos());
        assertEquals(387L, costoDeRuta(g, r.rutaFisicaCompleta()));
        assertTrue(r.rutaFisicaCompleta().size() >= r.ordenDeEntrega().size());
    }

    @Test
    @DisplayName("Reparto hacia el Litoral: 666 min (hay órdenes de entrega empatados) y la ruta física es una ruta real")
    void repartoLitoral() {
        GrafoMemoria g = GrafoPrueba.semilla();
        BranchAndBound.Resultado r = BranchAndBound.optimizarReparto(
                g, GrafoPrueba.i(g, "CABA"), indices(g, "ROSARIO", "SANTA_FE", "PARANA"), true);

        assertEquals(666L, r.costoTotalMinutos());
        assertEquals(5, r.ordenDeEntrega().size());
        assertEquals("CABA", r.ordenDeEntrega().get(0));
        assertEquals("CABA", r.ordenDeEntrega().get(4));
        assertEquals(Set.of("ROSARIO", "SANTA_FE", "PARANA"), new HashSet<>(r.ordenDeEntrega().subList(1, 4)));
        assertEquals(666L, costoDeRuta(g, r.rutaFisicaCompleta()));
    }

    @Test
    @DisplayName("Los 8 destinos de las cajas semilla: 1701 min, y la cota poda casi todo el árbol")
    void repartoCompletoDeLaSemilla() {
        GrafoMemoria g = GrafoPrueba.semilla();
        BranchAndBound.Resultado r = BranchAndBound.optimizarReparto(g, GrafoPrueba.i(g, "CABA"),
                indices(g, "QUILMES", "LA_PLATA", "PILAR", "ROSARIO", "CAMPANA", "SANTA_FE", "LOMAS_DE_ZAMORA", "CORRIENTES"),
                true);

        assertEquals(1701L, r.costoTotalMinutos());
        assertEquals(1701L, costoDeRuta(g, r.rutaFisicaCompleta()));
        assertEquals(109601L, r.nodosArbolCompleto(), "Σ 8!/(8-j)! para j = 0..8");
        assertTrue(r.nodosPodados() > 0, "la cota tiene que podar");
        assertTrue(r.nodosExplorados() < r.nodosArbolCompleto() / 10,
                "explora menos del 10% del árbol: " + r.nodosExplorados());
        assertTrue(r.costoSolucionInicial() >= r.costoTotalMinutos(), "el vecino más cercano nunca mejora al óptimo");
        assertTrue(r.porcentajeDelArbolExplorado() < 10.0);
    }

    @Test
    @DisplayName("Sin cota (fuerza bruta) se exploran TODOS los nodos del árbol y se llega al mismo óptimo")
    void sinCotaExploraTodo() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int[] destinos = indices(g, "QUILMES", "LA_PLATA", "PILAR", "CAMPANA", "ZARATE", "LOMAS_DE_ZAMORA", "ROSARIO");

        BranchAndBound.Resultado fuerzaBruta = BranchAndBound.optimizarReparto(g, GrafoPrueba.i(g, "CABA"), destinos, false);
        BranchAndBound.Resultado conCota = BranchAndBound.optimizarReparto(g, GrafoPrueba.i(g, "CABA"), destinos, true);

        assertFalse(fuerzaBruta.cotaActivada());
        assertNull(fuerzaBruta.costoSolucionInicial());
        assertEquals(0L, fuerzaBruta.nodosPodados());
        assertEquals(fuerzaBruta.nodosArbolCompleto(), fuerzaBruta.nodosExplorados());
        assertEquals(13700L, fuerzaBruta.nodosArbolCompleto(), "Σ 7!/(7-j)! para j = 0..7");
        assertEquals(fuerzaBruta.costoTotalMinutos(), conCota.costoTotalMinutos(), "la cota no puede cambiar el óptimo");
        assertTrue(conCota.nodosExplorados() < fuerzaBruta.nodosExplorados());
    }

    @Test
    @DisplayName("La cota es optimista: con ella se obtiene el mismo óptimo que sin ella en 60 grafos aleatorios")
    void cotaAdmisibleEnGrafosAleatorios() {
        Random azar = new Random(99);
        for (int caso = 0; caso < 60; caso++) {
            int n = 5 + azar.nextInt(4); // 5..8 vértices: origen + 4..7 destinos
            GrafoMemoria g = new GrafoMemoria();
            for (int i = 0; i < n; i++) {
                g.agregarVertice("N" + i);
            }
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (azar.nextInt(100) < 70 || j == i + 1) { // conexo: la cadena i,i+1 siempre está
                        g.agregarArista(i, j, 1 + azar.nextInt(60));
                    }
                }
            }
            g.ordenarVecinos();
            int[] destinos = new int[n - 1];
            for (int i = 1; i < n; i++) {
                destinos[i - 1] = i;
            }

            BranchAndBound.Resultado conCota = BranchAndBound.optimizarReparto(g, 0, destinos, true);
            BranchAndBound.Resultado sinCota = BranchAndBound.optimizarReparto(g, 0, destinos, false);

            assertEquals(sinCota.costoTotalMinutos(), conCota.costoTotalMinutos(), "caso " + caso);
            assertTrue(conCota.nodosExplorados() <= sinCota.nodosExplorados(), "caso " + caso);
        }
    }

    @Test
    @DisplayName("El tamaño del árbol completo es Σ k!/(k-j)!")
    void tamanioDelArbol() {
        assertEquals(1L, BranchAndBound.tamanioArbolCompleto(0));
        assertEquals(2L, BranchAndBound.tamanioArbolCompleto(1));
        assertEquals(16L, BranchAndBound.tamanioArbolCompleto(3));    // 1 + 3 + 6 + 6
        assertEquals(65L, BranchAndBound.tamanioArbolCompleto(4));    // 1 + 4 + 12 + 24 + 24
    }

    @Test
    @DisplayName("Valida destinos vacíos, repetidos, el origen como destino, demasiados destinos y destinos inalcanzables")
    void entradasInvalidas() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int caba = GrafoPrueba.i(g, "CABA");
        assertThrows(IllegalArgumentException.class, () -> BranchAndBound.optimizarReparto(g, caba, new int[0], true));
        assertThrows(IllegalArgumentException.class,
                () -> BranchAndBound.optimizarReparto(g, caba, indices(g, "PILAR", "PILAR"), true));
        assertThrows(IllegalArgumentException.class,
                () -> BranchAndBound.optimizarReparto(g, caba, indices(g, "CABA", "PILAR"), true));

        GrafoMemoria grande = new GrafoMemoria();
        for (int i = 0; i < 14; i++) {
            grande.agregarVertice("V" + i);
        }
        int[] demasiados = new int[BranchAndBound.MAX_DESTINOS + 1];
        for (int i = 0; i < demasiados.length; i++) {
            demasiados[i] = i + 1;
        }
        assertThrows(IllegalArgumentException.class, () -> BranchAndBound.optimizarReparto(grande, 0, demasiados, true));

        GrafoMemoria desconectado = new GrafoMemoria();
        int a = desconectado.agregarVertice("A");
        int b = desconectado.agregarVertice("B");
        assertThrows(IllegalArgumentException.class,
                () -> BranchAndBound.optimizarReparto(desconectado, a, new int[]{b}, true));
    }

    @Test
    @DisplayName("Un único destino: ida y vuelta")
    void unSoloDestino() {
        GrafoMemoria g = GrafoPrueba.semilla();
        BranchAndBound.Resultado r = BranchAndBound.optimizarReparto(
                g, GrafoPrueba.i(g, "CABA"), indices(g, "CORRIENTES"), true);
        assertEquals(2 * 747L, r.costoTotalMinutos());
        assertNotNull(r.cota());
        assertEquals(List.of("CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES",
                "RESISTENCIA", "SANTA_FE", "ROSARIO", "CABA"), r.rutaFisicaCompleta());
        assertEquals(2 * 747L, costoDeRuta(g, r.rutaFisicaCompleta()));
    }

    /** Suma los minutos de una ruta física caminando arista por arista sobre el grafo (la más barata si hay paralelas). */
    private static long costoDeRuta(GrafoMemoria g, List<String> ruta) {
        long total = 0;
        for (int i = 1; i < ruta.size(); i++) {
            int desde = GrafoPrueba.i(g, ruta.get(i - 1));
            int hasta = GrafoPrueba.i(g, ruta.get(i));
            long mejor = -1;
            for (GrafoMemoria.Arista a : g.vecinos(desde)) {
                if (a.destino() == hasta && (mejor < 0 || a.peso() < mejor)) {
                    mejor = a.peso();
                }
            }
            assertTrue(mejor >= 0, "no existe la arista " + ruta.get(i - 1) + " - " + ruta.get(i));
            total += mejor;
        }
        return total;
    }
}
