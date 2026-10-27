package com.perfumeria.logistica.model;

/**
 * Ítem para los algoritmos de capacidad: una caja de perfumes.
 *
 * @param id          identificador (ej. "PQ-03")
 * @param descripcion contenido de la caja
 * @param pesoKg      peso en kilogramos (entero &gt; 0)
 * @param valor       valor comercial en USD (entero &gt;= 0)
 * @param destinoId   id del nodo donde se entrega la caja
 */
public record Paquete(String id, String descripcion, int pesoKg, int valor, String destinoId) {
}
