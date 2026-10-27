package com.perfumeria.logistica.algorithm.estructura;

import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;
import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Ruta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grafo en memoria (lista de adyacencia). Es lo único que los algoritmos ven del dominio:
 * se construye una vez desde los datos de Neo4j y a partir de ahí NINGÚN algoritmo toca la base.
 *
 * <p>Los vértices se numeran 0..n-1. Soporta aristas no dirigidas ({@link #agregarArista}) y
 * arcos dirigidos ({@link #agregarArco}); el grafo de la red logística es no dirigido, los arcos
 * existen para poder probar Floyd-Warshall con ciclos negativos.</p>
 */
public final class GrafoMemoria {

    /** Valor "infinito" seguro para sumar dos veces sin desbordar un long. */
    public static final long INFINITO = Long.MAX_VALUE / 4;

    /** Entrada de la lista de adyacencia: hacia dónde va y cuánto cuesta. */
    public record Arista(int destino, int peso) {
    }

    /** Arista tal como fue agregada (una sola vez, aunque se guarde en ambas listas). */
    public record AristaGlobal(int origen, int destino, int peso, boolean dirigida) {
    }

    private final List<String> ids = new ArrayList<>();
    private final Map<String, Integer> indices = new HashMap<>();
    private final List<List<Arista>> adyacencia = new ArrayList<>();
    private final List<AristaGlobal> aristas = new ArrayList<>();
    private boolean hayPesosNegativos;
    private boolean hayArcosDirigidos;

    // ------------------------------------------------------------ construcción

    /**
     * Construye el grafo a partir del modelo. El resultado NO depende del orden de las listas de
     * entrada: los nodos se ordenan por id y los vecinos de cada nodo por (índice, peso), con
     * MergeSort propio. Así BFS/DFS dan siempre el mismo orden, venga de donde venga la lista.
     */
    public static GrafoMemoria desde(List<Nodo> nodos, List<Ruta> rutas) {
        Nodo[] ordenados = nodos.toArray(new Nodo[0]);
        Ordenamiento.mergeSort(ordenados, Comparator.comparing(Nodo::id));

        GrafoMemoria g = new GrafoMemoria();
        for (Nodo n : ordenados) {
            g.agregarVertice(n.id());
        }
        for (Ruta r : rutas) {
            int u = g.indice(r.origenId());
            int v = g.indice(r.destinoId());
            if (u < 0 || v < 0) {
                throw new IllegalArgumentException("La ruta " + r.origenId() + " - " + r.destinoId()
                        + " referencia un nodo que no existe");
            }
            g.agregarArista(u, v, r.minutos());
        }
        g.ordenarVecinos();
        return g;
    }

    public int agregarVertice(String id) {
        if (indices.containsKey(id)) {
            throw new IllegalArgumentException("Vértice duplicado: " + id);
        }
        int indice = ids.size();
        ids.add(id);
        indices.put(id, indice);
        adyacencia.add(new ArrayList<>());
        return indice;
    }

    /** Arista no dirigida: se puede recorrer en ambos sentidos. */
    public void agregarArista(int u, int v, int peso) {
        validarVertice(u);
        validarVertice(v);
        adyacencia.get(u).add(new Arista(v, peso));
        if (u != v) {
            adyacencia.get(v).add(new Arista(u, peso));
        }
        aristas.add(new AristaGlobal(u, v, peso, false));
        hayPesosNegativos |= peso < 0;
    }

    /** Arco dirigido u -> v. */
    public void agregarArco(int u, int v, int peso) {
        validarVertice(u);
        validarVertice(v);
        adyacencia.get(u).add(new Arista(v, peso));
        aristas.add(new AristaGlobal(u, v, peso, true));
        hayPesosNegativos |= peso < 0;
        hayArcosDirigidos = true;
    }

    /** Deja los vecinos de cada vértice ordenados por (destino, peso). O(E log E). */
    public void ordenarVecinos() {
        Comparator<Arista> porDestinoYPeso = Comparator.comparingInt(Arista::destino).thenComparingInt(Arista::peso);
        for (int u = 0; u < adyacencia.size(); u++) {
            Arista[] arreglo = adyacencia.get(u).toArray(new Arista[0]);
            Ordenamiento.mergeSort(arreglo, porDestinoYPeso);
            List<Arista> ordenada = adyacencia.get(u);
            ordenada.clear();
            Collections.addAll(ordenada, arreglo);
        }
    }

    // ------------------------------------------------------------ consulta

    public int numVertices() {
        return ids.size();
    }

    /** Cantidad de aristas/arcos agregados (cada arista no dirigida cuenta una vez). */
    public int numAristas() {
        return aristas.size();
    }

    public String id(int indice) {
        return ids.get(indice);
    }

    /** Índice del vértice con ese id, o -1 si no existe. */
    public int indice(String id) {
        Integer i = indices.get(id);
        return i == null ? -1 : i;
    }

    public List<String> ids() {
        return Collections.unmodifiableList(ids);
    }

    public List<Arista> vecinos(int u) {
        return Collections.unmodifiableList(adyacencia.get(u));
    }

    public List<AristaGlobal> aristas() {
        return Collections.unmodifiableList(aristas);
    }

    public boolean tienePesosNegativos() {
        return hayPesosNegativos;
    }

    public boolean tieneArcosDirigidos() {
        return hayArcosDirigidos;
    }

    private void validarVertice(int v) {
        if (v < 0 || v >= ids.size()) {
            throw new IllegalArgumentException("Vértice fuera de rango: " + v);
        }
    }
}
