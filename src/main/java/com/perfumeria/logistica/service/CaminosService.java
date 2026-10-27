package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.Backtracking;
import com.perfumeria.logistica.algorithm.Dijkstra;
import com.perfumeria.logistica.algorithm.FloydWarshall;
import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Caminos mínimos (Dijkstra, Floyd-Warshall) y rutas con restricciones (backtracking). */
@Service
public class CaminosService {

    public static final int LIMITE_MAXIMO_RUTAS_DEVUELTAS = 1000;

    private final GrafoService grafoService;

    public CaminosService(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    // ---------------------------------------------------------------- Dijkstra

    public Dijkstra.ResultadoCamino dijkstra(String origen, String destino) {
        GrafoMemoria g = grafoService.cargarGrafo();
        return Dijkstra.caminoMinimo(g, grafoService.resolverNodo(g, origen), grafoService.resolverNodo(g, destino));
    }

    // ---------------------------------------------------------------- Floyd-Warshall

    /**
     * Costo de correr Floyd-Warshall frente a correr Dijkstra desde cada vértice (hito 7).
     *
     * @param iteracionesFloydWarshall      V³: veces que se evaluó d[i][k] + d[k][j]
     * @param dijkstraNodosExpandidosTotal  suma de nodos expandidos en las V corridas de Dijkstra
     * @param dijkstraAristasExaminadasTotal suma de arcos examinados en las V corridas
     * @param resultadosCoinciden           las dos técnicas dieron exactamente las mismas distancias
     */
    public record ComparacionConDijkstra(int dijkstraEjecuciones, long iteracionesFloydWarshall,
                                         long actualizacionesFloydWarshall, long dijkstraNodosExpandidosTotal,
                                         long dijkstraAristasExaminadasTotal, boolean resultadosCoinciden) {
    }

    public record RespuestaTodosContraTodos(FloydWarshall.Resultado floydWarshall,
                                            ComparacionConDijkstra comparacionConDijkstraVVeces) {
    }

    public record RespuestaCaminoFloyd(String origen, String destino, boolean alcanzable, Long costoMinutos,
                                       List<String> camino) {
    }

    public RespuestaTodosContraTodos todosContraTodos() {
        GrafoMemoria g = grafoService.cargarGrafo();
        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);
        ComparacionConDijkstra comparacion = null;
        if (!fw.tieneCicloNegativo() && !g.tienePesosNegativos()) {
            comparacion = compararConDijkstra(g, fw);
        }
        return new RespuestaTodosContraTodos(fw.aResultado(), comparacion);
    }

    public RespuestaCaminoFloyd caminoFloyd(String origen, String destino) {
        GrafoMemoria g = grafoService.cargarGrafo();
        int o = grafoService.resolverNodo(g, origen);
        int d = grafoService.resolverNodo(g, destino);
        FloydWarshall.Matrices fw = FloydWarshall.calcular(g);
        if (fw.tieneCicloNegativo()) {
            throw new SolicitudInvalidaException("El grafo tiene un ciclo negativo: los caminos mínimos no están definidos");
        }
        boolean alcanzable = fw.costo(o, d) < GrafoMemoria.INFINITO;
        return new RespuestaCaminoFloyd(g.id(o), g.id(d), alcanzable, alcanzable ? fw.costo(o, d) : null,
                fw.caminoIds(o, d));
    }

    /**
     * Demostración de la detección de ciclos negativos sobre un grafo DIRIGIDO armado en memoria
     * (el grafo de la red logística no tiene pesos negativos): A→B (2), B→C (-5), C→A (1) suma -2.
     */
    public FloydWarshall.Resultado demoCicloNegativo() {
        GrafoMemoria g = new GrafoMemoria();
        int a = g.agregarVertice("A");
        int b = g.agregarVertice("B");
        int c = g.agregarVertice("C");
        int d = g.agregarVertice("D");
        g.agregarArco(a, b, 2);
        g.agregarArco(b, c, -5);
        g.agregarArco(c, a, 1);
        g.agregarArco(d, a, 4);
        return FloydWarshall.ejecutar(g);
    }

    private static ComparacionConDijkstra compararConDijkstra(GrafoMemoria g, FloydWarshall.Matrices fw) {
        int n = g.numVertices();
        long expandidos = 0;
        long examinadas = 0;
        boolean coinciden = true;
        for (int origen = 0; origen < n; origen++) {
            Dijkstra.Arbol arbol = Dijkstra.calcular(g, origen, -1);
            expandidos += arbol.nodosExpandidos();
            examinadas += arbol.aristasExaminadas();
            for (int destino = 0; destino < n; destino++) {
                if (arbol.distancia()[destino] != fw.costo(origen, destino)) {
                    coinciden = false;
                }
            }
        }
        return new ComparacionConDijkstra(n, fw.iteracionesInternas(), fw.actualizaciones(), expandidos, examinadas,
                coinciden);
    }

    // ---------------------------------------------------------------- Backtracking

    public Backtracking.Resultado rutasRestringidas(String origen, String destino, long maxMinutos, int maxSaltos,
                                                    int limite) {
        if (maxMinutos < 0) {
            throw new SolicitudInvalidaException("maxMinutos no puede ser negativo");
        }
        if (maxSaltos < 1) {
            throw new SolicitudInvalidaException("maxSaltos debe ser al menos 1");
        }
        if (limite < 1 || limite > LIMITE_MAXIMO_RUTAS_DEVUELTAS) {
            throw new SolicitudInvalidaException("limite debe estar entre 1 y " + LIMITE_MAXIMO_RUTAS_DEVUELTAS);
        }
        GrafoMemoria g = grafoService.cargarGrafo();
        int o = grafoService.resolverNodo(g, origen);
        int d = grafoService.resolverNodo(g, destino);
        if (o == d) {
            throw new SolicitudInvalidaException("El origen y el destino deben ser distintos");
        }
        return Backtracking.buscarRutas(g, o, d, maxMinutos, maxSaltos, limite);
    }
}
