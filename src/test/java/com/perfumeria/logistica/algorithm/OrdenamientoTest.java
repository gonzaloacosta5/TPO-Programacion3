package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.DatosSemilla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenamientoTest {

    private static final Comparator<Integer> NATURAL = Integer::compare;

    // ------------------------------------------------------------ propiedades comunes

    @Test
    @DisplayName("QuickSort ordena arreglos de todos los tamaños y formas")
    void quickSortEnTodasLasFormas() {
        verificarEnTodasLasFormas(Ordenamiento::quickSort);
    }

    @Test
    @DisplayName("MergeSort ordena arreglos de todos los tamaños y formas")
    void mergeSortEnTodasLasFormas() {
        verificarEnTodasLasFormas(Ordenamiento::mergeSort);
    }

    private static void verificarEnTodasLasFormas(BiConsumer<Integer[], Comparator<Integer>> ordenador) {
        Random azar = new Random(7);
        for (int n : new int[]{0, 1, 2, 3, 10, 17, 100, 1000, 5000}) {
            Integer[] aleatorio = new Integer[n];
            Integer[] ordenado = new Integer[n];
            Integer[] inverso = new Integer[n];
            Integer[] iguales = new Integer[n];
            Integer[] pocasClaves = new Integer[n];
            for (int i = 0; i < n; i++) {
                aleatorio[i] = azar.nextInt(10_000) - 5000;
                ordenado[i] = i;
                inverso[i] = n - i;
                iguales[i] = 42;
                pocasClaves[i] = azar.nextInt(4);
            }
            for (Integer[] caso : new Integer[][]{aleatorio, ordenado, inverso, iguales, pocasClaves}) {
                Map<Integer, Integer> antes = frecuencias(caso);
                ordenador.accept(caso, NATURAL);
                assertTrue(estaOrdenado(caso), "el arreglo de tamaño " + n + " quedó desordenado");
                assertEquals(antes, frecuencias(caso), "el ordenamiento debe conservar los mismos elementos");
            }
        }
    }

    @Test
    @DisplayName("QuickSort no se degrada con 100.000 claves iguales ni con entrada ya ordenada")
    void quickSortRobustoAnteEntradasAdversas() {
        int n = 100_000;
        Integer[] iguales = new Integer[n];
        Integer[] ordenado = new Integer[n];
        for (int i = 0; i < n; i++) {
            iguales[i] = 1;
            ordenado[i] = i;
        }
        Ordenamiento.quickSort(iguales, NATURAL);
        Ordenamiento.quickSort(ordenado, NATURAL);
        assertTrue(estaOrdenado(iguales));
        assertTrue(estaOrdenado(ordenado));
    }

    @Test
    @DisplayName("MergeSort es estable: los empates conservan su orden original")
    void mergeSortEsEstable() {
        record Par(int clave, int posicionOriginal) {
        }
        Random azar = new Random(3);
        Par[] pares = new Par[2000];
        for (int i = 0; i < pares.length; i++) {
            pares[i] = new Par(azar.nextInt(20), i);
        }
        Ordenamiento.mergeSort(pares, Comparator.comparingInt(Par::clave));
        for (int i = 1; i < pares.length; i++) {
            assertTrue(pares[i - 1].clave() <= pares[i].clave());
            if (pares[i - 1].clave() == pares[i].clave()) {
                assertTrue(pares[i - 1].posicionOriginal() < pares[i].posicionOriginal(),
                        "se rompió la estabilidad en la posición " + i);
            }
        }
    }

    @Test
    @DisplayName("Aceptan arreglos nulos o vacíos sin lanzar excepción")
    void nulosYVacios() {
        Ordenamiento.quickSort((Integer[]) null, NATURAL);
        Ordenamiento.mergeSort((Integer[]) null, NATURAL);
        Ordenamiento.quickSort(new Integer[0], NATURAL);
        Ordenamiento.mergeSort(new Integer[0], NATURAL);
    }

    // ------------------------------------------------------------ sobre los paquetes del grafo

    @Test
    @DisplayName("QuickSort ordena los paquetes por valor descendente")
    void quickSortSobrePaquetes() {
        Paquete[] paquetes = DatosSemilla.paquetes().toArray(new Paquete[0]);
        Ordenamiento.quickSort(paquetes, Comparator.comparingInt(Paquete::valor).reversed());
        // Valores: PQ-04=900, PQ-03=760, PQ-07=660, PQ-05=600, PQ-02=540, PQ-01=520, PQ-06=480, PQ-08=150
        assertEquals(List.of("PQ-04", "PQ-03", "PQ-07", "PQ-05", "PQ-02", "PQ-01", "PQ-06", "PQ-08"), ids(paquetes));
    }

    @Test
    @DisplayName("MergeSort ordena los paquetes por peso ascendente")
    void mergeSortSobrePaquetes() {
        Paquete[] paquetes = DatosSemilla.paquetes().toArray(new Paquete[0]);
        Ordenamiento.mergeSort(paquetes, Comparator.comparingInt(Paquete::pesoKg));
        assertEquals(List.of("PQ-08", "PQ-01", "PQ-07", "PQ-02", "PQ-05", "PQ-03", "PQ-06", "PQ-04"), ids(paquetes));
    }

    // ------------------------------------------------------------ auxiliares

    private static boolean estaOrdenado(Integer[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static Map<Integer, Integer> frecuencias(Integer[] a) {
        Map<Integer, Integer> m = new HashMap<>();
        for (Integer x : a) {
            m.merge(x, 1, Integer::sum);
        }
        return m;
    }

    private static List<String> ids(Paquete[] paquetes) {
        return java.util.Arrays.stream(paquetes).map(Paquete::id).toList();
    }
}
