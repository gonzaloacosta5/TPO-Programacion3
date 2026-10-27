package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.model.Paquete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mochila 0/1 por programación dinámica: qué cajas cargar en la camioneta para maximizar el
 * valor sin pasar la capacidad.
 *
 * <p>{@code dp[i][w]} = mejor valor usando solo las primeras {@code i} cajas con capacidad {@code w}:
 * <pre>
 *   dp[i][w] = dp[i-1][w]                                   si la caja i no entra (peso_i &gt; w)
 *   dp[i][w] = max(dp[i-1][w], dp[i-1][w-peso_i] + valor_i) si entra
 * </pre>
 * Las cajas elegidas se recuperan recorriendo la tabla desde dp[n][W] hacia atrás: si
 * {@code dp[i][w] != dp[i-1][w]}, la caja i fue tomada.</p>
 *
 * <p><b>Complejidad:</b> Θ(n·W) tiempo y Θ(n·W) espacio (pseudopolinomial: depende del valor numérico
 * de W, no de su cantidad de bits). Hace falta la tabla completa para recuperar los ítems; si solo
 * interesara el valor óptimo alcanzaría una fila: O(W) espacio.</p>
 */
public final class MochilaDP {

    /** Tope de celdas de la tabla (n+1)*(W+1) para no agotar la memoria con una capacidad enorme. */
    public static final long MAX_CELDAS = 5_000_000L;

    private MochilaDP() {
    }

    /**
     * @param tablaDp tabla dp completa (null si no se pidió): filas = cajas consideradas 0..n,
     *                columnas = capacidad 0..W
     */
    public record Resultado(String algoritmo, int capacidadKg, long valorOptimo, int pesoTotalKg,
                            List<Paquete> paquetesCargados, long[][] tablaDp) {
    }

    public static Resultado resolver(List<Paquete> paquetes, int capacidadKg, boolean incluirTabla) {
        if (capacidadKg < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa");
        }
        int n = paquetes.size();
        if ((long) (n + 1) * (capacidadKg + 1L) > MAX_CELDAS) {
            throw new IllegalArgumentException("La tabla dp tendría más de " + MAX_CELDAS
                    + " celdas; reducí la capacidad o la cantidad de paquetes");
        }
        for (Paquete p : paquetes) {
            if (p.pesoKg() <= 0) {
                throw new IllegalArgumentException("El paquete " + p.id() + " tiene peso no positivo");
            }
        }

        long[][] dp = new long[n + 1][capacidadKg + 1];
        for (int i = 1; i <= n; i++) {
            Paquete p = paquetes.get(i - 1);
            for (int w = 0; w <= capacidadKg; w++) {
                dp[i][w] = dp[i - 1][w];
                if (p.pesoKg() <= w) {
                    long conLaCaja = dp[i - 1][w - p.pesoKg()] + p.valor();
                    if (conLaCaja > dp[i][w]) {
                        dp[i][w] = conLaCaja;
                    }
                }
            }
        }

        // Recuperación de los ítems elegidos
        List<Paquete> elegidos = new ArrayList<>();
        int w = capacidadKg;
        int peso = 0;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                Paquete p = paquetes.get(i - 1);
                elegidos.add(p);
                w -= p.pesoKg();
                peso += p.pesoKg();
            }
        }
        Collections.reverse(elegidos); // volver al orden original de la lista

        return new Resultado("Mochila 0/1 (programación dinámica)", capacidadKg, dp[n][capacidadKg], peso,
                elegidos, incluirTabla ? dp : null);
    }
}
