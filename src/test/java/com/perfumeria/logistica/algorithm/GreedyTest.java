package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.DatosSemilla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreedyTest {

    /** Contraejemplo clásico: 50 kg de capacidad. A=10kg/60 (ratio 6), B=20kg/100 (5), C=30kg/120 (4). */
    private static List<Paquete> contraejemplo() {
        return List.of(
                new Paquete("A", "A", 10, 60, null),
                new Paquete("B", "B", 20, 100, null),
                new Paquete("C", "C", 30, 120, null));
    }

    @Test
    @DisplayName("El greedy por ratio toma A y B (160 USD) y se pierde el óptimo B + C (220 USD)")
    void elGreedyFallaEnElContraejemploClasico() {
        Greedy.Resultado greedy = Greedy.seleccionarPorRatio(contraejemplo(), 50);
        MochilaDP.Resultado optimo = MochilaDP.resolver(contraejemplo(), 50, false);

        assertEquals(List.of("A", "B", "C"), greedy.ordenConsiderado());
        assertEquals(List.of("A", "B"), greedy.paquetesCargados().stream().map(Paquete::id).toList());
        assertEquals(160L, greedy.valorTotal());
        assertEquals(30, greedy.pesoTotalKg());

        assertEquals(220L, optimo.valorOptimo());
        assertEquals(List.of("B", "C"), optimo.paquetesCargados().stream().map(Paquete::id).toList());
        assertTrue(greedy.valorTotal() < optimo.valorOptimo(), "el greedy NO es óptimo en este juego de datos");
    }

    @Test
    @DisplayName("Con la capacidad de la camioneta (40 kg) y las cajas semilla el greedy deja 90 USD sobre la mesa")
    void elGreedyFallaConLosDatosSemilla() {
        List<Paquete> semilla = DatosSemilla.paquetes();
        Greedy.Resultado greedy = Greedy.seleccionarPorRatio(semilla, DatosSemilla.CAPACIDAD_CAMIONETA_KG);
        MochilaDP.Resultado optimo = MochilaDP.resolver(semilla, DatosSemilla.CAPACIDAD_CAMIONETA_KG, false);

        // Ratios USD/kg: PQ-07=66, PQ-01=65, PQ-02=45, PQ-03=42,2, PQ-05=40, PQ-04=36, PQ-08=25, PQ-06=24
        assertEquals(List.of("PQ-07", "PQ-01", "PQ-02", "PQ-03", "PQ-05", "PQ-04", "PQ-08", "PQ-06"),
                greedy.ordenConsiderado());
        assertEquals(List.of("PQ-07", "PQ-01", "PQ-02", "PQ-08"),
                greedy.paquetesCargados().stream().map(Paquete::id).toList());
        assertEquals(36, greedy.pesoTotalKg());
        assertEquals(1870L, greedy.valorTotal());
        assertEquals(1960L, optimo.valorOptimo());
    }

    @Test
    @DisplayName("Si todo entra, el greedy carga todo y coincide con el óptimo")
    void todoEntra() {
        Greedy.Resultado greedy = Greedy.seleccionarPorRatio(DatosSemilla.paquetes(), 1000);
        assertEquals(8, greedy.paquetesCargados().size());
        assertEquals(4610L, greedy.valorTotal());
        assertEquals(114, greedy.pesoTotalKg());
    }

    @Test
    @DisplayName("Con capacidad 0 no carga nada")
    void capacidadCero() {
        Greedy.Resultado greedy = Greedy.seleccionarPorRatio(DatosSemilla.paquetes(), 0);
        assertTrue(greedy.paquetesCargados().isEmpty());
        assertEquals(0L, greedy.valorTotal());
    }

    @Test
    @DisplayName("Rechaza capacidades negativas y paquetes sin peso")
    void entradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> Greedy.seleccionarPorRatio(DatosSemilla.paquetes(), -1));
        List<Paquete> sinPeso = List.of(new Paquete("X", "X", 0, 10, null));
        assertThrows(IllegalArgumentException.class, () -> Greedy.seleccionarPorRatio(sinPeso, 10));
    }
}
