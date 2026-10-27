package com.perfumeria.logistica.model;

/**
 * Arista del grafo: una ruta terrestre BIDIRECCIONAL entre dos nodos.
 * Se guarda una sola vez en Neo4j (relación RUTA) y se recorre en ambos sentidos.
 *
 * @param origenId  id del primer extremo
 * @param destinoId id del segundo extremo
 * @param minutos   costo de la arista: tiempo de viaje en minutos (entero, &gt;= 0)
 */
public record Ruta(String origenId, String destinoId, int minutos) {
}
