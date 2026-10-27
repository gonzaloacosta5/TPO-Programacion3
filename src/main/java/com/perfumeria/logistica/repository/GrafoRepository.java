package com.perfumeria.logistica.repository;

import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.model.Ruta;

import java.util.List;

/**
 * Acceso a los datos del grafo. Es la ÚNICA capa que habla con Neo4j: los servicios
 * leen todo de acá una sola vez por solicitud y le pasan estructuras en memoria
 * a los algoritmos, que nunca consultan la base.
 */
public interface GrafoRepository {

    List<Nodo> obtenerNodos();

    List<Ruta> obtenerRutas();

    List<Paquete> obtenerPaquetes();

    long contarNodos();

    /** Borra los nodos Nodo/Paquete existentes y los reemplaza por los datos dados. */
    void reemplazarGrafo(List<Nodo> nodos, List<Ruta> rutas, List<Paquete> paquetes);
}
