package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;

import java.util.ArrayList;
import java.util.List;

/**
 * Branch &amp; Bound para el <b>reparto</b>: una camioneta sale del origen, debe entregar en TODOS los
 * destinos pedidos (en el orden que convenga) y vuelve al origen, minimizando los minutos totales.
 * Es un viajante de comercio (TSP) sobre los destinos, donde el costo entre dos puntos es el camino
 * mínimo de la red (se calcula con Floyd-Warshall en memoria antes de ramificar).
 *
 * <p><b>Ramificar:</b> cada nodo del árbol es una ruta parcial (origen, d1, ..., dk); sus hijos agregan
 * un destino todavía no visitado, en orden de menor costo primero para encontrar buenas soluciones
 * temprano.</p>
 *
 * <p><b>Cota (explícita y optimista):</b> para una ruta parcial que terminó en {@code v} con costo
 * {@code c} y un conjunto {@code R} de destinos pendientes,
 * <pre>
 *   cota = c + Σ_{u ∈ {v} ∪ R}  min{ costo(u, w) : w ∈ R ∪ {origen}, w ≠ u }
 * </pre>
 * Es OPTIMISTA (una cota inferior, nunca sobreestima) porque cualquier forma de completar la ruta hace
 * salir exactamente una vez a cada nodo de {v} ∪ R hacia algún nodo de R ∪ {origen}, y cada una de esas
 * salidas cuesta como mínimo la más barata disponible. Si {@code cota >= mejor solución conocida}, ninguna
 * completación puede mejorarla y la rama se PODA sin explorarla.</p>
 *
 * <p><b>Solución inicial:</b> el recorrido del vecino más cercano da una primera cota superior válida
 * ("incumbente"), de modo que se puede podar desde el primer nivel.</p>
 *
 * <p><b>Complejidad:</b> en el peor caso la cota no poda nada y se recorre todo el árbol: k destinos dan
 * Σ_j k!/(k-j)! ≈ e·k! nodos, cada uno con una cota O(k²) → O(k!·k²) tiempo; el costo de la poda es lo que
 * se gana en la práctica. Espacio O(k²) (matriz de costos entre destinos) + O(k) de recursión.</p>
 */
public final class BranchAndBound {

    public static final int MAX_DESTINOS = 11;
    public static final long LIMITE_NODOS = 50_000_000L;

    public static final String DESCRIPCION_COTA =
            "cota = costo acumulado + suma, para cada nodo pendiente (y el actual), de su salida más barata hacia "
                    + "un nodo pendiente o hacia el origen. Es optimista (cota inferior) porque cada nodo debe salir "
                    + "exactamente una vez y nunca por menos que su salida más barata.";

    private BranchAndBound() {
    }

    /**
     * @param ordenDeEntrega                      origen, destinos en el orden óptimo y origen de nuevo
     * @param rutaFisicaCompleta                  la misma ruta expandida a todos los nodos intermedios de la red
     * @param costoSolucionInicial                costo del vecino más cercano (null si no se usó la cota)
     * @param nodosExplorados                     nodos del árbol de búsqueda efectivamente generados
     * @param nodosPodados                        ramas descartadas por la cota (cada una era la raíz de un subárbol)
     * @param nodosArbolCompleto                  tamaño del árbol sin poda: Σ k!/(k-j)!
     * @param porcentajeDelArbolExplorado         100 · explorados / arbolCompleto
     */
    public record Resultado(String origen, List<String> destinos, List<String> ordenDeEntrega,
                            long costoTotalMinutos, List<String> rutaFisicaCompleta, boolean cotaActivada,
                            String cota, Long costoSolucionInicial, long nodosExplorados, long nodosPodados,
                            long nodosArbolCompleto, double porcentajeDelArbolExplorado) {
    }

    /**
     * @param usarCota false desactiva la poda (fuerza bruta): sirve para verificar que la cota no cambia el óptimo
     */
    public static Resultado optimizarReparto(GrafoMemoria g, int origen, int[] destinos, boolean usarCota) {
        validar(g, origen, destinos);

        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);
        if (fw.tieneCicloNegativo()) {
            throw new IllegalArgumentException("El grafo tiene un ciclo negativo: el reparto no está definido");
        }

        int k = destinos.length;
        int m = k + 1; // posición 0 = origen, 1..k = destinos
        int[] nodos = new int[m];
        nodos[0] = origen;
        System.arraycopy(destinos, 0, nodos, 1, k);

        long[][] c = new long[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                long costo = fw.costo(nodos[i], nodos[j]);
                if (costo >= GrafoMemoria.INFINITO) {
                    throw new IllegalArgumentException("No existe ruta entre " + g.id(nodos[i]) + " y "
                            + g.id(nodos[j]) + ": el reparto es inviable");
                }
                c[i][j] = costo;
            }
        }

        Busqueda b = new Busqueda(c, usarCota);
        if (usarCota) {
            b.inicializarConVecinoMasCercano();
        }
        long costoInicial = b.mejor;
        b.expandir(0, 1, 0, 0L);

        List<String> orden = new ArrayList<>();
        List<String> fisica = new ArrayList<>();
        fisica.add(g.id(origen));
        for (int i = 0; i < m; i++) {
            orden.add(g.id(nodos[b.mejorOrden[i]]));
        }
        orden.add(g.id(origen));
        for (int i = 0; i < m; i++) {
            int desde = nodos[b.mejorOrden[i]];
            int hasta = nodos[b.mejorOrden[(i + 1) % m]];
            List<Integer> tramo = fw.camino(desde, hasta);
            for (int t = 1; t < tramo.size(); t++) {
                fisica.add(g.id(tramo.get(t)));
            }
        }

        long arbolCompleto = tamanioArbolCompleto(k);
        double porcentaje = Math.round(1_000_000.0 * b.explorados / arbolCompleto) / 10_000.0;
        List<String> pedidos = new ArrayList<>();
        for (int d : destinos) {
            pedidos.add(g.id(d));
        }
        return new Resultado(g.id(origen), pedidos, orden, b.mejor, fisica, usarCota,
                usarCota ? DESCRIPCION_COTA : "sin cota (fuerza bruta)", usarCota ? costoInicial : null,
                b.explorados, b.podados, arbolCompleto, porcentaje);
    }

    /** Σ_{j=0..k} k!/(k-j)! = cantidad de nodos del árbol de permutaciones parciales sin podar. */
    static long tamanioArbolCompleto(int k) {
        long termino = 1;
        long total = 1;
        for (int j = 1; j <= k; j++) {
            termino *= (k - j + 1);
            total += termino;
        }
        return total;
    }

    private static void validar(GrafoMemoria g, int origen, int[] destinos) {
        if (destinos == null || destinos.length == 0) {
            throw new IllegalArgumentException("Hay que indicar al menos un destino");
        }
        if (destinos.length > MAX_DESTINOS) {
            throw new IllegalArgumentException("Máximo " + MAX_DESTINOS + " destinos por reparto");
        }
        for (int i = 0; i < destinos.length; i++) {
            if (destinos[i] == origen) {
                throw new IllegalArgumentException("El origen no puede ser también un destino");
            }
            for (int j = 0; j < i; j++) {
                if (destinos[i] == destinos[j]) {
                    throw new IllegalArgumentException("Hay destinos repetidos: " + g.id(destinos[i]));
                }
            }
        }
    }

    private static final class Busqueda {
        final long[][] c;
        final int m;
        final boolean usarCota;
        long mejor = GrafoMemoria.INFINITO;
        int[] mejorOrden;
        final int[] ruta;
        long explorados;
        long podados;

        Busqueda(long[][] c, boolean usarCota) {
            this.c = c;
            this.m = c.length;
            this.usarCota = usarCota;
            this.mejorOrden = new int[m];
            this.ruta = new int[m];
        }

        /** Primera solución factible: ir siempre al destino pendiente más cercano. */
        void inicializarConVecinoMasCercano() {
            boolean[] visitado = new boolean[m];
            int[] orden = new int[m];
            visitado[0] = true;
            int actual = 0;
            long costo = 0;
            for (int paso = 1; paso < m; paso++) {
                int elegido = -1;
                for (int v = 1; v < m; v++) {
                    if (!visitado[v] && (elegido < 0 || c[actual][v] < c[actual][elegido])) {
                        elegido = v;
                    }
                }
                visitado[elegido] = true;
                orden[paso] = elegido;
                costo += c[actual][elegido];
                actual = elegido;
            }
            costo += c[actual][0];
            mejor = costo;
            mejorOrden = orden;
        }

        /**
         * @param actual   última posición de la ruta parcial
         * @param visitados máscara de bits de posiciones ya visitadas (el bit 0, el origen, siempre está)
         * @param nivel    cantidad de destinos ya visitados
         * @param costo    minutos acumulados
         */
        void expandir(int actual, int visitados, int nivel, long costo) {
            explorados++;
            if (explorados > LIMITE_NODOS) {
                throw new LimiteComputoExcedidoException("El branch and bound superó el límite de "
                        + LIMITE_NODOS + " nodos explorados; pedí menos destinos");
            }
            if (nivel == m - 1) {
                long total = costo + c[actual][0]; // todos entregados: volver al origen
                if (total < mejor) {
                    mejor = total;
                    System.arraycopy(ruta, 0, mejorOrden, 0, m);
                }
                return;
            }

            // Hijos = destinos pendientes, ordenados por costo creciente desde 'actual' (inserción).
            int[] hijos = new int[m];
            int cantidad = 0;
            for (int v = 1; v < m; v++) {
                if ((visitados & (1 << v)) != 0) {
                    continue;
                }
                int pos = cantidad++;
                while (pos > 0 && c[actual][hijos[pos - 1]] > c[actual][v]) {
                    hijos[pos] = hijos[pos - 1];
                    pos--;
                }
                hijos[pos] = v;
            }

            for (int i = 0; i < cantidad; i++) {
                int v = hijos[i];
                long nuevoCosto = costo + c[actual][v];
                int nuevosVisitados = visitados | (1 << v);
                if (usarCota && nuevoCosto + cotaRestante(v, nuevosVisitados) >= mejor) {
                    podados++; // la mejor completación posible no mejora lo que ya tenemos
                    continue;
                }
                ruta[nivel + 1] = v;
                expandir(v, nuevosVisitados, nivel + 1, nuevoCosto);
            }
        }

        /** Cota inferior del costo que falta desde {@code v} para entregar lo pendiente y volver al origen. */
        long cotaRestante(int v, int visitados) {
            long suma = 0;
            for (int u = 0; u < m; u++) {
                boolean debeSalir = u == v || (u != 0 && (visitados & (1 << u)) == 0);
                if (!debeSalir) {
                    continue;
                }
                long minimo = GrafoMemoria.INFINITO;
                for (int w = 0; w < m; w++) {
                    boolean destinoValido = w == 0 || (visitados & (1 << w)) == 0;
                    if (w != u && destinoValido && c[u][w] < minimo) {
                        minimo = c[u][w];
                    }
                }
                suma += minimo;
            }
            return suma;
        }
    }
}
