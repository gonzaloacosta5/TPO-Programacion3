package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.MinHeap;
import com.perfumeria.logistica.algorithm.estructura.UnionFind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstructurasTest {

    @Test
    @DisplayName("MinHeap extrae las claves de menor a mayor")
    void heapExtraeEnOrden() {
        MinHeap heap = new MinHeap();
        long[] claves = {5, 3, 8, 1, 9, 2, 7, 3};
        for (int i = 0; i < claves.length; i++) {
            heap.insertar(claves[i], i);
        }
        long[] esperado = {1, 2, 3, 3, 5, 7, 8, 9};
        for (long e : esperado) {
            assertEquals(e, heap.extraerMinimo().clave());
        }
        assertTrue(heap.estaVacio());
    }

    @Test
    @DisplayName("MinHeap devuelve el valor asociado a cada clave y crece más allá de su capacidad inicial")
    void heapConMuchosElementos() {
        MinHeap heap = new MinHeap();
        Random azar = new Random(42);
        int n = 5000;
        for (int i = 0; i < n; i++) {
            long clave = azar.nextInt(1000);
            heap.insertar(clave, (int) clave * 10);
        }
        assertEquals(n, heap.tamanio());
        long anterior = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            MinHeap.Entrada e = heap.extraerMinimo();
            assertTrue(e.clave() >= anterior, "las claves deben salir en orden no decreciente");
            assertEquals(e.clave() * 10, e.valor());
            anterior = e.clave();
        }
    }

    @Test
    @DisplayName("MinHeap vacío lanza excepción al extraer")
    void heapVacio() {
        assertThrows(NoSuchElementException.class, () -> new MinHeap().extraerMinimo());
    }

    @Test
    @DisplayName("UnionFind une conjuntos, detecta ciclos y cuenta componentes")
    void unionFind() {
        UnionFind uf = new UnionFind(6);
        assertEquals(6, uf.componentes());
        assertTrue(uf.unir(0, 1));
        assertTrue(uf.unir(2, 3));
        assertTrue(uf.unir(1, 3));
        assertEquals(3, uf.componentes());
        assertTrue(uf.conectados(0, 2));
        assertFalse(uf.conectados(0, 4));
        assertFalse(uf.unir(0, 3), "0 y 3 ya están conectados: unirlos cerraría un ciclo");
        assertEquals(3, uf.componentes());
        assertTrue(uf.unir(4, 5));
        assertTrue(uf.unir(0, 5));
        assertEquals(1, uf.componentes());
    }

    @Test
    @DisplayName("UnionFind soporta cadenas largas (compresión de caminos)")
    void unionFindCadenaLarga() {
        int n = 200_000;
        UnionFind uf = new UnionFind(n);
        for (int i = 1; i < n; i++) {
            uf.unir(i - 1, i);
        }
        assertEquals(1, uf.componentes());
        assertTrue(uf.conectados(0, n - 1));
    }
}
