package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;
import com.perfumeria.logistica.model.Paquete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enfoque goloso para cargar la camioneta: ordenar las cajas por valor/peso (de mayor a menor)
 * y cargar cada una si todavía entra.
 *
 * <p>Es rápido pero NO siempre óptimo: la decisión "me quedo con la caja de mejor ratio" es
 * irrevocable y puede dejar un hueco que ninguna otra caja llena. Ver
 * {@code MochilaDP} para la solución exacta y los tests/endpoint de contraejemplo.</p>
 *
 * <p><b>Complejidad:</b> ordenar O(n log n) (MergeSort propio) + una pasada O(n).
 * Total O(n log n) tiempo, O(n) espacio.</p>
 */
public final class Greedy {

    private Greedy() {
    }

    public record Resultado(String algoritmo, int capacidadKg, int pesoTotalKg, long valorTotal,
                            List<Paquete> paquetesCargados, List<String> ordenConsiderado) {
    }

    public static Resultado seleccionarPorRatio(List<Paquete> paquetes, int capacidadKg) {
        if (capacidadKg < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa");
        }
        for (Paquete p : paquetes) {
            if (p.pesoKg() <= 0) {
                throw new IllegalArgumentException("El paquete " + p.id() + " tiene peso no positivo");
            }
        }

        Paquete[] arreglo = paquetes.toArray(new Paquete[0]);
        Ordenamiento.mergeSort(arreglo, RATIO_DESCENDENTE);

        List<Paquete> cargados = new ArrayList<>();
        List<String> orden = new ArrayList<>();
        int peso = 0;
        long valor = 0;
        for (Paquete p : arreglo) {
            orden.add(p.id());
            if (peso + p.pesoKg() <= capacidadKg) {
                cargados.add(p);
                peso += p.pesoKg();
                valor += p.valor();
            }
        }
        return new Resultado("Greedy por ratio valor/peso", capacidadKg, peso, valor, cargados, orden);
    }

    /**
     * Ratio valor/peso descendente SIN dividir (productos cruzados en long, sin errores de redondeo).
     * Ante empate de ratio, desempata por id para que el resultado sea determinista.
     */
    static final Comparator<Paquete> RATIO_DESCENDENTE = (a, b) -> {
        int porRatio = Long.compare((long) b.valor() * a.pesoKg(), (long) a.valor() * b.pesoKg());
        return porRatio != 0 ? porRatio : a.id().compareTo(b.id());
    };
}
