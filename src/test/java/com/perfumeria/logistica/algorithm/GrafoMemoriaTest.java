package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Ruta;
import com.perfumeria.logistica.model.TipoNodo;
import com.perfumeria.logistica.repository.DatosSemilla;
import com.perfumeria.logistica.soporte.GrafoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafoMemoriaTest {

    @Test
    @DisplayName("La semilla genera 12 vértices y 17 aristas no dirigidas con pesos no negativos")
    void semillaTieneElTamanioEsperado() {
        GrafoMemoria g = GrafoPrueba.semilla();
        assertEquals(12, g.numVertices());
        assertEquals(17, g.numAristas());
        assertFalse(g.tienePesosNegativos());
        assertFalse(g.tieneArcosDirigidos());
    }

    @Test
    @DisplayName("Los vértices se numeran por orden alfabético de id")
    void indicesPorOrdenAlfabetico() {
        GrafoMemoria g = GrafoPrueba.semilla();
        assertEquals("CABA", g.id(0));
        assertEquals("CAMPANA", g.id(1));
        assertEquals("ZARATE", g.id(11));
        assertEquals(-1, g.indice("NO_EXISTE"));
    }

    @Test
    @DisplayName("Los vecinos quedan ordenados y no dependen del orden de las listas de entrada")
    void vecinosDeterministas() {
        List<Nodo> nodosAlReves = new ArrayList<>(DatosSemilla.nodos());
        List<Ruta> rutasAlReves = new ArrayList<>(DatosSemilla.rutas());
        Collections.reverse(nodosAlReves);
        Collections.reverse(rutasAlReves);

        GrafoMemoria normal = GrafoPrueba.semilla();
        GrafoMemoria alReves = GrafoMemoria.desde(nodosAlReves, rutasAlReves);

        List<String> esperados = List.of("CAMPANA", "LOMAS_DE_ZAMORA", "PILAR", "QUILMES", "ROSARIO");
        for (GrafoMemoria g : List.of(normal, alReves)) {
            List<String> vecinosDeCaba = new ArrayList<>();
            for (GrafoMemoria.Arista a : g.vecinos(g.indice("CABA"))) {
                vecinosDeCaba.add(g.id(a.destino()));
            }
            assertEquals(esperados, vecinosDeCaba);
        }
        assertEquals(Recorridos.bfs(normal, 0).ordenVisita(), Recorridos.bfs(alReves, 0).ordenVisita());
    }

    @Test
    @DisplayName("Las aristas no dirigidas se pueden recorrer en ambos sentidos con el mismo peso")
    void aristasBidireccionales() {
        GrafoMemoria g = GrafoPrueba.semilla();
        int quilmes = g.indice("QUILMES");
        int lomas = g.indice("LOMAS_DE_ZAMORA");
        assertTrue(g.vecinos(quilmes).contains(new GrafoMemoria.Arista(lomas, 25)));
        assertTrue(g.vecinos(lomas).contains(new GrafoMemoria.Arista(quilmes, 25)));
    }

    @Test
    @DisplayName("Una ruta que apunta a un nodo inexistente es rechazada")
    void rutaConNodoInexistente() {
        List<Nodo> nodos = List.of(new Nodo("A", "A", TipoNodo.DEPOSITO));
        List<Ruta> rutas = List.of(new Ruta("A", "B", 5));
        assertThrows(IllegalArgumentException.class, () -> GrafoMemoria.desde(nodos, rutas));
    }

    @Test
    @DisplayName("No se permiten vértices duplicados")
    void verticeDuplicado() {
        GrafoMemoria g = new GrafoMemoria();
        g.agregarVertice("A");
        assertThrows(IllegalArgumentException.class, () -> g.agregarVertice("A"));
    }
}
