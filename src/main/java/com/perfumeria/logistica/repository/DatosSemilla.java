package com.perfumeria.logistica.repository;

import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.model.Ruta;
import com.perfumeria.logistica.model.TipoNodo;

import java.util.List;

/**
 * Datos de ejemplo de la red logística: 12 nodos, 17 rutas y 8 cajas.
 *
 * <p>Los tiempos de viaje son estimaciones redondeadas (ruta + autopista, sin tránsito)
 * pensadas para trabajar los algoritmos, no para planificar un viaje real. Todos los
 * pesos de las rutas son distintos entre sí a propósito: así el árbol de expansión
 * mínima es único y se puede verificar a mano.</p>
 */
public final class DatosSemilla {

    public static final int CAPACIDAD_CAMIONETA_KG = 40;

    private DatosSemilla() {
    }

    public static List<Nodo> nodos() {
        return List.of(
                new Nodo("CABA", "Depósito Central CABA", TipoNodo.DEPOSITO),
                new Nodo("ROSARIO", "Depósito Regional Rosario", TipoNodo.DEPOSITO),
                new Nodo("QUILMES", "Centro de Distribución Quilmes", TipoNodo.CENTRO_DISTRIBUCION),
                new Nodo("LA_PLATA", "Centro de Distribución La Plata", TipoNodo.CENTRO_DISTRIBUCION),
                new Nodo("PILAR", "Centro de Distribución Pilar", TipoNodo.CENTRO_DISTRIBUCION),
                new Nodo("SANTA_FE", "Centro de Distribución Santa Fe", TipoNodo.CENTRO_DISTRIBUCION),
                new Nodo("RESISTENCIA", "Centro de Distribución Resistencia", TipoNodo.CENTRO_DISTRIBUCION),
                new Nodo("LOMAS_DE_ZAMORA", "Punto de Retiro Lomas de Zamora", TipoNodo.PUNTO_RETIRO),
                new Nodo("CAMPANA", "Punto de Retiro Campana", TipoNodo.PUNTO_RETIRO),
                new Nodo("ZARATE", "Punto de Retiro Zárate", TipoNodo.PUNTO_RETIRO),
                new Nodo("PARANA", "Punto de Retiro Paraná", TipoNodo.PUNTO_RETIRO),
                new Nodo("CORRIENTES", "Punto de Retiro Corrientes", TipoNodo.PUNTO_RETIRO)
        );
    }

    public static List<Ruta> rutas() {
        return List.of(
                // Conurbano y GBA
                new Ruta("CABA", "LOMAS_DE_ZAMORA", 32),
                new Ruta("CABA", "QUILMES", 40),
                new Ruta("LOMAS_DE_ZAMORA", "QUILMES", 25),
                new Ruta("LOMAS_DE_ZAMORA", "LA_PLATA", 60),
                new Ruta("QUILMES", "LA_PLATA", 45),
                // Zona norte
                new Ruta("CABA", "PILAR", 55),
                new Ruta("CABA", "CAMPANA", 80),
                new Ruta("PILAR", "CAMPANA", 35),
                new Ruta("CAMPANA", "ZARATE", 20),
                // Corredor hacia el Litoral
                new Ruta("CABA", "ROSARIO", 200),
                new Ruta("ZARATE", "ROSARIO", 120),
                new Ruta("ROSARIO", "SANTA_FE", 105),
                new Ruta("ROSARIO", "PARANA", 150),
                new Ruta("SANTA_FE", "PARANA", 28),
                // Hacia el NEA
                new Ruta("SANTA_FE", "RESISTENCIA", 420),
                new Ruta("PARANA", "CORRIENTES", 450),
                new Ruta("RESISTENCIA", "CORRIENTES", 22)
        );
    }

    /**
     * Con {@link #CAPACIDAD_CAMIONETA_KG} kg el greedy por ratio valor/peso NO es óptimo:
     * elige PQ-07, PQ-01, PQ-02 y PQ-08 (36 kg, 1870 USD) y el óptimo es PQ-02, PQ-03 y PQ-07
     * (40 kg, 1960 USD).
     */
    public static List<Paquete> paquetes() {
        return List.of(
                new Paquete("PQ-01", "Caja Perfumes de Nicho x4", 8, 520, "QUILMES"),
                new Paquete("PQ-02", "Caja Eau de Parfum Dama x6", 12, 540, "LA_PLATA"),
                new Paquete("PQ-03", "Caja Eau de Toilette Caballero x12", 18, 760, "PILAR"),
                new Paquete("PQ-04", "Caja Colonias Familiares x24", 25, 900, "ROSARIO"),
                new Paquete("PQ-05", "Caja Sets de Regalo x8", 15, 600, "CAMPANA"),
                new Paquete("PQ-06", "Caja Body Mist x30", 20, 480, "SANTA_FE"),
                new Paquete("PQ-07", "Caja Perfumes Árabes x6", 10, 660, "LOMAS_DE_ZAMORA"),
                new Paquete("PQ-08", "Caja Testers x40", 6, 150, "CORRIENTES")
        );
    }
}
