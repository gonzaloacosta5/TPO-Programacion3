package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.DatosSemilla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MochilaDPTest {

    @Test
    @DisplayName("Mochila 0/1 sobre las cajas semilla con 40 kg: PQ-02 + PQ-03 + PQ-07 = 1960 USD (óptimo único)")
    void optimoSobreLaSemilla() {
        MochilaDP.Resultado r = MochilaDP.resolver(DatosSemilla.paquetes(), 40, true);

        assertEquals(1960L, r.valorOptimo());
        assertEquals(40, r.pesoTotalKg());
        assertEquals(List.of("PQ-02", "PQ-03", "PQ-07"), r.paquetesCargados().stream().map(Paquete::id).toList());
    }

    @Test
    @DisplayName("La tabla dp tiene (n+1) x (W+1) celdas, fila 0 y columna 0 en cero y dp[n][W] = valor óptimo")
    void formaDeLaTabla() {
        MochilaDP.Resultado r = MochilaDP.resolver(DatosSemilla.paquetes(), 40, true);
        long[][] dp = r.tablaDp();

        assertEquals(9, dp.length);
        assertEquals(41, dp[0].length);
        for (int w = 0; w <= 40; w++) {
            assertEquals(0L, dp[0][w]);
        }
        for (int i = 0; i <= 8; i++) {
            assertEquals(0L, dp[i][0]);
        }
        assertEquals(r.valorOptimo(), dp[8][40]);
        // Con solo PQ-01 (8 kg, 520 USD) y capacidad 8 o más se obtiene 520
        assertEquals(0L, dp[1][7]);
        assertEquals(520L, dp[1][8]);
        assertEquals(520L, dp[1][40]);
        // Monotonía: más capacidad o más cajas nunca empeora el valor
        for (int i = 1; i <= 8; i++) {
            for (int w = 1; w <= 40; w++) {
                assertTrue(dp[i][w] >= dp[i][w - 1]);
                assertTrue(dp[i][w] >= dp[i - 1][w]);
            }
        }
    }

    @Test
    @DisplayName("Sin pedir la tabla no se devuelve (ahorra payload)")
    void sinTabla() {
        assertNull(MochilaDP.resolver(DatosSemilla.paquetes(), 40, false).tablaDp());
    }

    @Test
    @DisplayName("Coincide con la fuerza bruta en 300 instancias aleatorias")
    void contraFuerzaBruta() {
        Random azar = new Random(2026);
        for (int caso = 0; caso < 300; caso++) {
            int n = 1 + azar.nextInt(10);
            List<Paquete> paquetes = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                paquetes.add(new Paquete("P" + i, "P" + i, 1 + azar.nextInt(15), azar.nextInt(100), null));
            }
            int capacidad = azar.nextInt(60);

            MochilaDP.Resultado r = MochilaDP.resolver(paquetes, capacidad, false);

            assertEquals(mejorPorFuerzaBruta(paquetes, capacidad), r.valorOptimo(), "caso " + caso);
            // Los ítems recuperados deben ser coherentes con el valor y respetar la capacidad
            long valorRecuperado = 0;
            int pesoRecuperado = 0;
            for (Paquete p : r.paquetesCargados()) {
                valorRecuperado += p.valor();
                pesoRecuperado += p.pesoKg();
            }
            assertEquals(r.valorOptimo(), valorRecuperado, "caso " + caso);
            assertEquals(r.pesoTotalKg(), pesoRecuperado, "caso " + caso);
            assertTrue(pesoRecuperado <= capacidad, "caso " + caso);
        }
    }

    @Test
    @DisplayName("Capacidad 0 o ninguna caja: valor 0 y nada cargado")
    void casosBorde() {
        MochilaDP.Resultado sinCapacidad = MochilaDP.resolver(DatosSemilla.paquetes(), 0, false);
        assertEquals(0L, sinCapacidad.valorOptimo());
        assertTrue(sinCapacidad.paquetesCargados().isEmpty());

        MochilaDP.Resultado sinCajas = MochilaDP.resolver(List.of(), 50, false);
        assertEquals(0L, sinCajas.valorOptimo());
    }

    @Test
    @DisplayName("Rechaza capacidades negativas, pesos no positivos y tablas gigantes")
    void entradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> MochilaDP.resolver(DatosSemilla.paquetes(), -5, false));
        List<Paquete> sinPeso = List.of(new Paquete("X", "X", 0, 10, null));
        assertThrows(IllegalArgumentException.class, () -> MochilaDP.resolver(sinPeso, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> MochilaDP.resolver(DatosSemilla.paquetes(), 2_000_000_000, false));
    }

    private static long mejorPorFuerzaBruta(List<Paquete> paquetes, int capacidad) {
        int n = paquetes.size();
        long mejor = 0;
        for (int mascara = 0; mascara < (1 << n); mascara++) {
            int peso = 0;
            long valor = 0;
            for (int i = 0; i < n; i++) {
                if ((mascara & (1 << i)) != 0) {
                    peso += paquetes.get(i).pesoKg();
                    valor += paquetes.get(i).valor();
                }
            }
            if (peso <= capacidad && valor > mejor) {
                mejor = valor;
            }
        }
        return mejor;
    }
}
