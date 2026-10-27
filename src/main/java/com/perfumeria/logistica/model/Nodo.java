package com.perfumeria.logistica.model;

/**
 * Vértice del grafo: un depósito, un centro de distribución o un punto de retiro.
 *
 * @param id     identificador estable en MAYÚSCULAS sin tildes (ej. "LA_PLATA")
 * @param nombre nombre legible (ej. "Centro de Distribución La Plata")
 * @param tipo   rol del nodo en la red
 */
public record Nodo(String id, String nombre, TipoNodo tipo) {
}
