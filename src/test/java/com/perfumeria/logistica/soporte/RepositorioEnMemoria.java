package com.perfumeria.logistica.soporte;

import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.model.Ruta;
import com.perfumeria.logistica.repository.DatosSemilla;
import com.perfumeria.logistica.repository.GrafoRepository;

import java.util.ArrayList;
import java.util.List;

/** Repositorio falso (sin Neo4j) para probar servicios con los datos semilla. */
public final class RepositorioEnMemoria implements GrafoRepository {

    private List<Nodo> nodos;
    private List<Ruta> rutas;
    private List<Paquete> paquetes;

    /** Arranca cargado con la semilla. */
    public RepositorioEnMemoria() {
        this(DatosSemilla.nodos(), DatosSemilla.rutas(), DatosSemilla.paquetes());
    }

    public RepositorioEnMemoria(List<Nodo> nodos, List<Ruta> rutas, List<Paquete> paquetes) {
        reemplazarGrafo(nodos, rutas, paquetes);
    }

    public static RepositorioEnMemoria vacio() {
        return new RepositorioEnMemoria(List.of(), List.of(), List.of());
    }

    @Override
    public List<Nodo> obtenerNodos() {
        return nodos;
    }

    @Override
    public List<Ruta> obtenerRutas() {
        return rutas;
    }

    @Override
    public List<Paquete> obtenerPaquetes() {
        return paquetes;
    }

    @Override
    public long contarNodos() {
        return nodos.size();
    }

    @Override
    public void reemplazarGrafo(List<Nodo> nodos, List<Ruta> rutas, List<Paquete> paquetes) {
        this.nodos = new ArrayList<>(nodos);
        this.rutas = new ArrayList<>(rutas);
        this.paquetes = new ArrayList<>(paquetes);
    }
}
