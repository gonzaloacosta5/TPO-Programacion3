package com.perfumeria.logistica.algorithm.estructura;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Min-heap binario propio (cola de prioridad) con claves {@code long} y valores {@code int}.
 * No usa java.util.PriorityQueue: es la estructura que sostiene la complejidad O((V+E) log V)
 * de Dijkstra y Prim.
 *
 * <p>No tiene decrease-key: el que lo necesite inserta una entrada nueva y descarta las
 * obsoletas al extraerlas ("lazy deletion"). Insertar y extraer cuestan O(log n); consultar el
 * mínimo O(1).</p>
 */
public final class MinHeap {

    /** Par (clave, valor) extraído del heap. */
    public record Entrada(long clave, int valor) {
    }

    private long[] claves = new long[16];
    private int[] valores = new int[16];
    private int tamanio;

    public boolean estaVacio() {
        return tamanio == 0;
    }

    public int tamanio() {
        return tamanio;
    }

    /** O(log n) amortizado. */
    public void insertar(long clave, int valor) {
        if (tamanio == claves.length) {
            claves = Arrays.copyOf(claves, tamanio * 2);
            valores = Arrays.copyOf(valores, tamanio * 2);
        }
        claves[tamanio] = clave;
        valores[tamanio] = valor;
        subir(tamanio);
        tamanio++;
    }

    /** O(log n). */
    public Entrada extraerMinimo() {
        if (tamanio == 0) {
            throw new NoSuchElementException("El heap está vacío");
        }
        Entrada minimo = new Entrada(claves[0], valores[0]);
        tamanio--;
        if (tamanio > 0) {
            claves[0] = claves[tamanio];
            valores[0] = valores[tamanio];
            bajar(0);
        }
        return minimo;
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (claves[padre] <= claves[i]) {
                break;
            }
            intercambiar(padre, i);
            i = padre;
        }
    }

    private void bajar(int i) {
        while (true) {
            int izq = 2 * i + 1;
            int der = izq + 1;
            int menor = i;
            if (izq < tamanio && claves[izq] < claves[menor]) {
                menor = izq;
            }
            if (der < tamanio && claves[der] < claves[menor]) {
                menor = der;
            }
            if (menor == i) {
                return;
            }
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int i, int j) {
        long c = claves[i];
        claves[i] = claves[j];
        claves[j] = c;
        int v = valores[i];
        valores[i] = valores[j];
        valores[j] = v;
    }
}
