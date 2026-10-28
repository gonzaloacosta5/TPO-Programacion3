package com.perfumeria.logistica.algorithm;

import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.DatosSemilla;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchAndBoundCargaTest {

    @Test
    @DisplayName("Las 8 cajas semilla en 3 camionetas: el greedy LPT deja 40 kg en la más cargada y el B&B baja a 39")
    void semillaEnTresCamionetas() {
        BranchAndBoundCarga.Resultado r = BranchAndBoundCarga.repartir(DatosSemilla.paquetes(), 3, true);

        assertEquals(114L, r.pesoTotalKg());
        assertEquals(38L, r.cotaInferiorKg(), "⌈114 / 3⌉");
        assertEquals(40L, r.cargaMaximaSolucionInicialKg());
        assertEquals(39L, r.cargaMaximaKg(), "38 es imposible: con la caja de 25 kg ninguna combinación suma 38");
        assertRepartoValido(DatosSemilla.paquetes(), r);
        assertEquals(9841L, r.nodosArbolCompleto(), "Σ 3^i para i = 0..8");
        assertTrue(r.nodosPodados() > 0, "la cota tiene que podar");
        assertTrue(r.nodosExplorados() < r.nodosArbolCompleto());
    }

    @Test
    @DisplayName("Sin cota (fuerza bruta) se exploran TODOS los nodos del árbol y se llega al mismo óptimo")
    void sinCotaExploraTodo() {
        BranchAndBoundCarga.Resultado fuerzaBruta = BranchAndBoundCarga.repartir(DatosSemilla.paquetes(), 3, false);

        assertFalse(fuerzaBruta.cotaActivada());
        assertNull(fuerzaBruta.cargaMaximaSolucionInicialKg());
        assertEquals(0L, fuerzaBruta.nodosPodados());
        assertEquals(fuerzaBruta.nodosArbolCompleto(), fuerzaBruta.nodosExplorados());
        assertEquals(39L, fuerzaBruta.cargaMaximaKg());
    }

    @Test
    @DisplayName("La cota es optimista: con ella se obtiene el mismo óptimo que sin ella en 60 repartos aleatorios")
    void cotaAdmisibleEnRepartosAleatorios() {
        Random azar = new Random(2026);
        for (int caso = 0; caso < 60; caso++) {
            int n = 1 + azar.nextInt(8); // 1..8 cajas
            int k = 1 + azar.nextInt(4); // 1..4 camionetas
            List<Paquete> cajas = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                cajas.add(new Paquete("C" + i, "Caja " + i, 1 + azar.nextInt(30), 1, null));
            }

            BranchAndBoundCarga.Resultado conCota = BranchAndBoundCarga.repartir(cajas, k, true);
            BranchAndBoundCarga.Resultado sinCota = BranchAndBoundCarga.repartir(cajas, k, false);

            assertEquals(sinCota.cargaMaximaKg(), conCota.cargaMaximaKg(), "caso " + caso);
            assertTrue(conCota.nodosExplorados() <= sinCota.nodosExplorados(), "caso " + caso);
            assertTrue(conCota.cargaMaximaKg() >= conCota.cotaInferiorKg(), "caso " + caso);
            assertRepartoValido(cajas, conCota);
        }
    }

    @Test
    @DisplayName("Casos borde: una camioneta lleva todo y con más camionetas que cajas manda la caja más pesada")
    void casosBorde() {
        List<Paquete> semilla = DatosSemilla.paquetes();
        assertEquals(114L, BranchAndBoundCarga.repartir(semilla, 1, true).cargaMaximaKg());

        BranchAndBoundCarga.Resultado sobran = BranchAndBoundCarga.repartir(semilla, 10, true);
        assertEquals(25L, sobran.cargaMaximaKg(), "la caja más pesada (PQ-04) no se puede partir");
        assertRepartoValido(semilla, sobran);

        List<Paquete> unaCaja = List.of(new Paquete("U", "Única", 7, 1, null));
        assertEquals(7L, BranchAndBoundCarga.repartir(unaCaja, 3, true).cargaMaximaKg());
    }

    @Test
    @DisplayName("El tamaño del árbol completo es Σ k^i")
    void tamanioDelArbol() {
        assertEquals(1L, BranchAndBoundCarga.tamanioArbolCompleto(0, 3));
        assertEquals(4L, BranchAndBoundCarga.tamanioArbolCompleto(3, 1));    // un camino: 1 + 1 + 1 + 1
        assertEquals(15L, BranchAndBoundCarga.tamanioArbolCompleto(3, 2));   // 1 + 2 + 4 + 8
        assertEquals(9841L, BranchAndBoundCarga.tamanioArbolCompleto(8, 3)); // (3^9 - 1) / 2
    }

    @Test
    @DisplayName("Valida la cantidad de camionetas, la lista vacía, demasiadas cajas y pesos no positivos")
    void entradasInvalidas() {
        List<Paquete> semilla = DatosSemilla.paquetes();
        assertThrows(IllegalArgumentException.class, () -> BranchAndBoundCarga.repartir(semilla, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> BranchAndBoundCarga.repartir(semilla, BranchAndBoundCarga.MAX_CAMIONETAS + 1, true));
        assertThrows(IllegalArgumentException.class, () -> BranchAndBoundCarga.repartir(List.of(), 3, true));
        assertThrows(IllegalArgumentException.class,
                () -> BranchAndBoundCarga.repartir(List.of(new Paquete("X", "X", 0, 1, null)), 3, true));

        List<Paquete> demasiadas = new ArrayList<>();
        for (int i = 0; i <= BranchAndBoundCarga.MAX_CAJAS; i++) {
            demasiadas.add(new Paquete("D" + i, "D" + i, 1, 1, null));
        }
        assertThrows(IllegalArgumentException.class, () -> BranchAndBoundCarga.repartir(demasiadas, 3, true));
    }

    /** Cada caja viaja en exactamente una camioneta, las cargas suman bien y la carga máxima informada es la real. */
    private static void assertRepartoValido(List<Paquete> cajas, BranchAndBoundCarga.Resultado r) {
        Set<String> vistas = new HashSet<>();
        long maxima = 0;
        for (BranchAndBoundCarga.Camioneta c : r.reparto()) {
            long suma = 0;
            for (Paquete p : c.paquetes()) {
                assertTrue(vistas.add(p.id()), "la caja " + p.id() + " está en dos camionetas");
                suma += p.pesoKg();
            }
            assertEquals(suma, c.cargaKg(), "camioneta " + c.numero());
            maxima = Math.max(maxima, suma);
        }
        assertEquals(cajas.size(), vistas.size(), "todas las cajas tienen que viajar");
        assertEquals(maxima, r.cargaMaximaKg());
        assertEquals(r.camionetas(), r.reparto().size());
    }
}
