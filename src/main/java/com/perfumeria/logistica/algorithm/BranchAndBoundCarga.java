package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;
import com.perfumeria.logistica.model.Paquete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Branch &amp; Bound para repartir la <b>carga</b>: las cajas se reparten entre {@code k} camionetas iguales
 * minimizando la carga de la camioneta más cargada (problema de <i>makespan</i>), para que ninguna salga
 * sobrecargada. Complementa a {@link BranchAndBound}, que decide en qué orden entregar.
 *
 * <p><b>Ramificar:</b> las cajas se ubican de la más pesada a la más liviana; un nodo de nivel {@code i} ya
 * decidió la camioneta de las primeras {@code i} cajas, y sus {@code k} hijos prueban cada camioneta para la
 * caja {@code i}.</p>
 *
 * <p><b>Cota (explícita y optimista):</b> para un nodo cuyas camionetas llevan las cargas {@code c_1..c_k},
 * <pre>
 *   cota = max( max_j c_j , ⌈P / k⌉ )        P = peso total de todas las cajas
 * </pre>
 * Es OPTIMISTA (una cota inferior, nunca sobreestima) porque las cargas solo crecen al agregar cajas, así que
 * la camioneta más cargada ya no baja de {@code max c_j}; y entre las k camionetas se reparten P kg, así que
 * alguna lleva al menos el promedio P/k (al menos ⌈P/k⌉, porque los pesos son enteros). Si
 * {@code cota >= mejor reparto conocido}, ninguna completación puede mejorarlo y la rama se PODA.</p>
 *
 * <p><b>Solución inicial:</b> el greedy LPT (cada caja, de la más pesada a la más liviana, a la camioneta
 * menos cargada) da una primera cota superior, de modo que se puede podar desde el primer nivel.</p>
 *
 * <p><b>Complejidad:</b> en el peor caso la cota no poda nada y se recorre todo el árbol: n cajas y k
 * camionetas dan Σ_{i=0..n} k^i nodos, O(k^n), y cada hijo calcula su cota en O(1) porque la carga máxima se
 * lleva acumulada. Ordenar las cajas cuesta O(n log n). Espacio O(n + k): recursión de profundidad n, la
 * asignación actual y las cargas de las k camionetas.</p>
 */
public final class BranchAndBoundCarga {

    public static final int MAX_CAJAS = 16;
    public static final int MAX_CAMIONETAS = 10;
    public static final long LIMITE_NODOS = 50_000_000L;

    public static final String DESCRIPCION_COTA =
            "cota = máximo entre la carga de la camioneta más cargada hasta ahora y el peso total dividido la "
                    + "cantidad de camionetas (redondeado hacia arriba). Es optimista (cota inferior) porque las "
                    + "cargas solo crecen al agregar cajas y alguna camioneta tiene que llevar al menos el promedio.";

    private BranchAndBoundCarga() {
    }

    /** Una camioneta del reparto: cuántos kg lleva y qué cajas. */
    public record Camioneta(int numero, long cargaKg, List<Paquete> paquetes) {
    }

    /**
     * @param cargaMaximaKg                carga de la camioneta más cargada en el reparto óptimo (lo que se minimiza)
     * @param cotaInferiorKg               ⌈peso total / camionetas⌉: ningún reparto puede bajar de este valor
     * @param cargaMaximaSolucionInicialKg carga máxima del greedy LPT (null si no se usó la cota)
     * @param nodosExplorados              nodos del árbol de búsqueda efectivamente generados
     * @param nodosPodados                 ramas descartadas por la cota (cada una era la raíz de un subárbol)
     * @param nodosArbolCompleto           tamaño del árbol sin poda: Σ k^i
     * @param porcentajeDelArbolExplorado  100 · explorados / arbolCompleto
     */
    public record Resultado(int camionetas, long pesoTotalKg, long cargaMaximaKg, long cotaInferiorKg,
                            List<Camioneta> reparto, boolean cotaActivada, String cota,
                            Long cargaMaximaSolucionInicialKg, long nodosExplorados, long nodosPodados,
                            long nodosArbolCompleto, double porcentajeDelArbolExplorado) {
    }

    /**
     * @param usarCota false desactiva la poda (fuerza bruta): sirve para verificar que la cota no cambia el óptimo
     */
    public static Resultado repartir(List<Paquete> paquetes, int camionetas, boolean usarCota) {
        validar(paquetes, camionetas);

        Paquete[] cajas = paquetes.toArray(new Paquete[0]);
        Ordenamiento.mergeSort(cajas, MAS_PESADA_PRIMERO);
        int n = cajas.length;
        int[] pesos = new int[n];
        long total = 0;
        for (int i = 0; i < n; i++) {
            pesos[i] = cajas[i].pesoKg();
            total += pesos[i];
        }
        long cotaInferior = (total + camionetas - 1) / camionetas; // ⌈P / k⌉

        Busqueda b = new Busqueda(pesos, camionetas, cotaInferior, usarCota);
        if (usarCota) {
            b.inicializarConLpt();
        }
        long cargaInicial = b.mejor;
        b.expandir(0, 0L);

        List<List<Paquete>> porCamioneta = new ArrayList<>();
        for (int j = 0; j < camionetas; j++) {
            porCamioneta.add(new ArrayList<>());
        }
        long[] cargas = new long[camionetas];
        for (int i = 0; i < n; i++) {
            int j = b.mejorAsignacion[i];
            porCamioneta.get(j).add(cajas[i]);
            cargas[j] += pesos[i];
        }
        List<Camioneta> reparto = new ArrayList<>();
        for (int j = 0; j < camionetas; j++) {
            reparto.add(new Camioneta(j + 1, cargas[j], porCamioneta.get(j)));
        }

        long arbolCompleto = tamanioArbolCompleto(n, camionetas);
        double porcentaje = Math.round(1_000_000.0 * b.explorados / arbolCompleto) / 10_000.0;
        return new Resultado(camionetas, total, b.mejor, cotaInferior, reparto, usarCota,
                usarCota ? DESCRIPCION_COTA : "sin cota (fuerza bruta)", usarCota ? cargaInicial : null,
                b.explorados, b.podados, arbolCompleto, porcentaje);
    }

    /** Σ_{i=0..n} k^i = cantidad de nodos del árbol de decisiones sin podar. */
    static long tamanioArbolCompleto(int n, int k) {
        long termino = 1;
        long total = 1;
        for (int i = 1; i <= n; i++) {
            termino *= k;
            total += termino;
        }
        return total;
    }

    /** La más pesada primero; ante empate, por id, para que el resultado sea determinista. */
    static final Comparator<Paquete> MAS_PESADA_PRIMERO = (a, b) -> {
        int porPeso = Integer.compare(b.pesoKg(), a.pesoKg());
        return porPeso != 0 ? porPeso : a.id().compareTo(b.id());
    };

    private static void validar(List<Paquete> paquetes, int camionetas) {
        if (camionetas < 1 || camionetas > MAX_CAMIONETAS) {
            throw new IllegalArgumentException("La cantidad de camionetas debe estar entre 1 y " + MAX_CAMIONETAS);
        }
        if (paquetes == null || paquetes.isEmpty()) {
            throw new IllegalArgumentException("Hay que indicar al menos una caja para repartir");
        }
        if (paquetes.size() > MAX_CAJAS) {
            throw new IllegalArgumentException("Máximo " + MAX_CAJAS + " cajas por reparto");
        }
        for (Paquete p : paquetes) {
            if (p.pesoKg() <= 0) {
                throw new IllegalArgumentException("El paquete " + p.id() + " tiene peso no positivo");
            }
        }
    }

    private static final class Busqueda {
        final int[] pesos;
        final int k;
        final long cotaInferior;
        final boolean usarCota;
        final long[] cargas;
        final int[] asignacion;
        final int[] mejorAsignacion;
        long mejor = Long.MAX_VALUE;
        long explorados;
        long podados;

        Busqueda(int[] pesos, int k, long cotaInferior, boolean usarCota) {
            this.pesos = pesos;
            this.k = k;
            this.cotaInferior = cotaInferior;
            this.usarCota = usarCota;
            this.cargas = new long[k];
            this.asignacion = new int[pesos.length];
            this.mejorAsignacion = new int[pesos.length];
        }

        /** Primera solución factible (greedy LPT): cada caja, de mayor a menor peso, a la camioneta menos cargada. */
        void inicializarConLpt() {
            long[] carga = new long[k];
            for (int i = 0; i < pesos.length; i++) {
                int menosCargada = 0;
                for (int j = 1; j < k; j++) {
                    if (carga[j] < carga[menosCargada]) {
                        menosCargada = j;
                    }
                }
                carga[menosCargada] += pesos[i];
                mejorAsignacion[i] = menosCargada;
            }
            long maxima = 0;
            for (long c : carga) {
                maxima = Math.max(maxima, c);
            }
            mejor = maxima;
        }

        /**
         * @param i           próxima caja a ubicar (las anteriores ya tienen camioneta)
         * @param cargaMaxima carga de la camioneta más cargada hasta ahora
         */
        void expandir(int i, long cargaMaxima) {
            explorados++;
            if (explorados > LIMITE_NODOS) {
                throw new LimiteComputoExcedidoException("El branch and bound superó el límite de "
                        + LIMITE_NODOS + " nodos explorados; probá con menos camionetas");
            }
            if (i == pesos.length) {
                if (cargaMaxima < mejor) { // todas las cajas ubicadas: reparto completo
                    mejor = cargaMaxima;
                    System.arraycopy(asignacion, 0, mejorAsignacion, 0, pesos.length);
                }
                return;
            }
            for (int j = 0; j < k; j++) {
                long nuevaCarga = cargas[j] + pesos[i];
                long nuevaMaxima = Math.max(cargaMaxima, nuevaCarga);
                if (usarCota && Math.max(nuevaMaxima, cotaInferior) >= mejor) {
                    podados++; // ni en el mejor caso esta rama mejora el reparto que ya tenemos
                    continue;
                }
                cargas[j] = nuevaCarga;
                asignacion[i] = j;
                expandir(i + 1, nuevaMaxima);
                cargas[j] -= pesos[i]; // backtrack: deshacer la decisión al volver
            }
        }
    }
}
