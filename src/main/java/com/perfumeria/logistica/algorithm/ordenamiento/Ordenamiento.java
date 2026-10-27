package com.perfumeria.logistica.algorithm.ordenamiento;

import java.util.Comparator;

/**
 * QuickSort y MergeSort implementados desde cero (sin Collections.sort / Arrays.sort / streams).
 * Ambos ordenan el arreglo recibido IN-PLACE según el comparador.
 *
 * <table>
 *   <caption>Resumen de complejidad</caption>
 *   <tr><th></th><th>Tiempo</th><th>Espacio extra</th><th>Estable</th></tr>
 *   <tr><td>QuickSort</td><td>O(n log n) esperado, O(n²) peor caso</td><td>O(log n)</td><td>No</td></tr>
 *   <tr><td>MergeSort</td><td>Θ(n log n) siempre</td><td>O(n)</td><td>Sí</td></tr>
 * </table>
 */
public final class Ordenamiento {

    private Ordenamiento() {
    }

    // ------------------------------------------------------------------ QuickSort

    /**
     * QuickSort con pivote = mediana de tres y partición de tres vías (Dijkstra), que mantiene
     * O(n log n) aun con muchas claves repetidas. Recursa sobre la mitad MÁS CHICA y itera sobre
     * la otra, así la pila nunca supera O(log n).
     */
    public static <T> void quickSort(T[] a, Comparator<? super T> cmp) {
        if (a == null || a.length < 2) {
            return;
        }
        quickSort(a, 0, a.length - 1, cmp);
    }

    private static <T> void quickSort(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        while (lo < hi) {
            T pivote = medianaDeTres(a, lo, hi, cmp);

            // Invariante: a[lo..lt-1] < pivote, a[lt..i-1] == pivote, a[gt+1..hi] > pivote
            int lt = lo;
            int gt = hi;
            int i = lo;
            while (i <= gt) {
                int c = cmp.compare(a[i], pivote);
                if (c < 0) {
                    intercambiar(a, lt++, i++);
                } else if (c > 0) {
                    intercambiar(a, i, gt--);
                } else {
                    i++;
                }
            }
            // El bloque [lt..gt] (iguales al pivote) ya está en su lugar definitivo.
            if (lt - lo < hi - gt) {
                quickSort(a, lo, lt - 1, cmp);
                lo = gt + 1;
            } else {
                quickSort(a, gt + 1, hi, cmp);
                hi = lt - 1;
            }
        }
    }

    private static <T> T medianaDeTres(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        T x = a[lo];
        T y = a[lo + (hi - lo) / 2];
        T z = a[hi];
        if (cmp.compare(x, y) < 0) {
            if (cmp.compare(y, z) < 0) {
                return y;
            }
            return cmp.compare(x, z) < 0 ? z : x;
        }
        if (cmp.compare(x, z) < 0) {
            return x;
        }
        return cmp.compare(y, z) < 0 ? z : y;
    }

    private static <T> void intercambiar(T[] a, int i, int j) {
        T tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    // ------------------------------------------------------------------ MergeSort

    /**
     * MergeSort top-down estable: ante empate conserva el orden original (en la mezcla toma
     * del lado izquierdo salvo que el derecho sea estrictamente menor). Usa un único arreglo
     * auxiliar de tamaño n.
     */
    public static <T> void mergeSort(T[] a, Comparator<? super T> cmp) {
        if (a == null || a.length < 2) {
            return;
        }
        @SuppressWarnings("unchecked")
        T[] aux = (T[]) new Object[a.length];
        mergeSort(a, aux, 0, a.length - 1, cmp);
    }

    private static <T> void mergeSort(T[] a, T[] aux, int lo, int hi, Comparator<? super T> cmp) {
        if (lo >= hi) {
            return;
        }
        int mid = lo + (hi - lo) / 2;
        mergeSort(a, aux, lo, mid, cmp);
        mergeSort(a, aux, mid + 1, hi, cmp);
        if (cmp.compare(a[mid], a[mid + 1]) <= 0) {
            return; // las dos mitades ya forman una secuencia ordenada
        }
        mezclar(a, aux, lo, mid, hi, cmp);
    }

    private static <T> void mezclar(T[] a, T[] aux, int lo, int mid, int hi, Comparator<? super T> cmp) {
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }
        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = aux[j++];
            } else if (j > hi) {
                a[k] = aux[i++];
            } else if (cmp.compare(aux[j], aux[i]) < 0) {
                a[k] = aux[j++];
            } else {
                a[k] = aux[i++];
            }
        }
    }
}
