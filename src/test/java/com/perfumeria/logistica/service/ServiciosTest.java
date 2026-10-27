package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.BranchAndBound;
import com.perfumeria.logistica.algorithm.Greedy;
import com.perfumeria.logistica.algorithm.ResultadoMst;
import com.perfumeria.logistica.exception.RecursoNoEncontradoException;
import com.perfumeria.logistica.exception.SolicitudInvalidaException;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.DatosSemilla;
import com.perfumeria.logistica.soporte.RepositorioEnMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba la cadena completa servicio -> algoritmo con un repositorio en memoria (sin Neo4j). */
class ServiciosTest {

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final GrafoService grafoService = new GrafoService(repositorio);
    private final CaminosService caminos = new CaminosService(grafoService);
    private final PaquetesService paquetes = new PaquetesService(repositorio);
    private final RepartoService reparto = new RepartoService(grafoService, repositorio);

    // ------------------------------------------------------------ grafo

    @Test
    @DisplayName("El resumen del grafo informa 12 vértices, 17 aristas, 8 paquetes y la distribución por tipo")
    void resumen() {
        GrafoService.ResumenGrafo r = grafoService.resumen();
        assertEquals(12, r.vertices());
        assertEquals(17, r.aristas());
        assertEquals(8, r.paquetes());
        assertEquals("minutos", r.unidadCosto());
        assertFalse(r.dirigido());
        assertEquals(Map.of("DEPOSITO", 2, "CENTRO_DISTRIBUCION", 5, "PUNTO_RETIRO", 5), r.nodosPorTipo());
    }

    @Test
    @DisplayName("Los ids de nodo se aceptan en cualquier capitalización, con espacios y con tildes")
    void normalizacionDeIds() {
        var g = grafoService.cargarGrafo();
        assertEquals(g.indice("LA_PLATA"), grafoService.resolverNodo(g, "la plata"));
        assertEquals(g.indice("ZARATE"), grafoService.resolverNodo(g, "Zárate"));
        assertEquals(g.indice("CABA"), grafoService.resolverNodo(g, "  caba "));
    }

    @Test
    @DisplayName("Un nodo inexistente lanza RecursoNoEncontradoException y lista los nodos válidos")
    void nodoInexistente() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> new RecorridoService(grafoService).bfs("ATLANTIDA"));
        assertTrue(ex.getMessage().contains("ATLANTIDA"));
        assertTrue(ex.getMessage().contains("CABA"));
    }

    @Test
    @DisplayName("Si la base está vacía se informa con un error manejado")
    void grafoVacio() {
        GrafoService vacio = new GrafoService(RepositorioEnMemoria.vacio());
        assertThrows(RecursoNoEncontradoException.class, vacio::cargarGrafo);
    }

    // ------------------------------------------------------------ recorridos, caminos, MST

    @Test
    @DisplayName("BFS y DFS resuelven el nodo de origen y recorren el grafo cargado")
    void recorridos() {
        RecorridoService servicio = new RecorridoService(grafoService);
        assertEquals(12, servicio.bfs("caba").nodosVisitados());
        assertEquals(12, servicio.dfs("CABA").nodosVisitados());
    }

    @Test
    @DisplayName("Dijkstra vía servicio: CABA -> CORRIENTES = 747")
    void dijkstra() {
        assertEquals(747L, caminos.dijkstra("caba", "corrientes").costoMinutos());
    }

    @Test
    @DisplayName("Todos contra todos: Floyd-Warshall (12³ iteraciones) coincide con 12 corridas de Dijkstra")
    void floydContraDijkstra() {
        CaminosService.RespuestaTodosContraTodos r = caminos.todosContraTodos();
        assertFalse(r.floydWarshall().tieneCicloNegativo());
        CaminosService.ComparacionConDijkstra c = r.comparacionConDijkstraVVeces();
        assertNotNull(c);
        assertTrue(c.resultadosCoinciden());
        assertEquals(12, c.dijkstraEjecuciones());
        assertEquals(1728L, c.iteracionesFloydWarshall());
        assertEquals(144L, c.dijkstraNodosExpandidosTotal(), "12 corridas x 12 nodos expandidos");
        assertEquals(12 * 2 * 17L, c.dijkstraAristasExaminadasTotal(), "cada arista se examina desde sus dos extremos");
    }

    @Test
    @DisplayName("Camino mínimo por Floyd-Warshall y demo de ciclo negativo")
    void floydCaminoYDemo() {
        CaminosService.RespuestaCaminoFloyd r = caminos.caminoFloyd("CABA", "CORRIENTES");
        assertTrue(r.alcanzable());
        assertEquals(747L, r.costoMinutos());
        assertEquals(List.of("CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES"), r.camino());

        assertTrue(caminos.demoCicloNegativo().tieneCicloNegativo());
    }

    @Test
    @DisplayName("Backtracking vía servicio valida parámetros y devuelve los conteos con y sin poda")
    void backtracking() {
        var r = caminos.rutasRestringidas("caba", "quilmes", 100, 3, 20);
        assertEquals(2, r.totalRutasValidas());
        assertTrue(r.nodosExploradosConPoda() < r.nodosExploradosSinPoda());

        assertThrows(SolicitudInvalidaException.class, () -> caminos.rutasRestringidas("CABA", "CABA", 100, 3, 20));
        assertThrows(SolicitudInvalidaException.class, () -> caminos.rutasRestringidas("CABA", "PILAR", -1, 3, 20));
        assertThrows(SolicitudInvalidaException.class, () -> caminos.rutasRestringidas("CABA", "PILAR", 100, 0, 20));
        assertThrows(SolicitudInvalidaException.class, () -> caminos.rutasRestringidas("CABA", "PILAR", 100, 3, 0));
    }

    @Test
    @DisplayName("Prim (origen por defecto) y Kruskal dan 907 minutos")
    void mst() {
        ArbolExpansionService servicio = new ArbolExpansionService(grafoService);
        ResultadoMst prim = servicio.prim(null);
        assertEquals(907L, prim.costoTotalMinutos());
        assertEquals("CABA", prim.origen());
        assertEquals(907L, servicio.prim("rosario").costoTotalMinutos());
        assertEquals(907L, servicio.kruskal().costoTotalMinutos());
    }

    // ------------------------------------------------------------ paquetes

    @Test
    @DisplayName("QuickSort y MergeSort devuelven exactamente el mismo orden para todos los criterios")
    void ordenamientosCoinciden() {
        for (String criterio : List.of("valor", "peso", "ratio", "id")) {
            for (String orden : List.of("asc", "desc")) {
                PaquetesService.RespuestaOrdenamiento q = paquetes.ordenarConQuickSort(criterio, orden);
                PaquetesService.RespuestaOrdenamiento m = paquetes.ordenarConMergeSort(criterio, orden);
                assertEquals(m.paquetes(), q.paquetes(), criterio + " " + orden);
                assertEquals(8, q.paquetes().size());
                assertTrue(q.comparaciones() > 0 && m.comparaciones() > 0);
            }
        }
    }

    @Test
    @DisplayName("Ordenar por valor descendente deja primero la caja más cara")
    void ordenarPorValor() {
        List<Paquete> ordenados = paquetes.ordenarConMergeSort("valor", "desc").paquetes();
        assertEquals("PQ-04", ordenados.get(0).id());
        assertEquals("PQ-08", ordenados.get(7).id());
        assertEquals("PQ-07", paquetes.ordenarConQuickSort("ratio", "desc").paquetes().get(0).id());
    }

    @Test
    @DisplayName("Criterio u orden inválidos son una solicitud inválida")
    void ordenamientoInvalido() {
        assertThrows(SolicitudInvalidaException.class, () -> paquetes.ordenarConQuickSort("color", "asc"));
        assertThrows(SolicitudInvalidaException.class, () -> paquetes.ordenarConMergeSort("valor", "arriba"));
    }

    @Test
    @DisplayName("Greedy con 40 kg: 1870 USD contra 1960 del óptimo, o sea 90 USD perdidos")
    void greedyContraOptimo() {
        PaquetesService.RespuestaGreedy r = paquetes.greedy(DatosSemilla.CAPACIDAD_CAMIONETA_KG);
        assertEquals(1870L, r.greedy().valorTotal());
        assertEquals(1960L, r.comparacionConOptimo().valorOptimoMochila());
        assertEquals(90L, r.comparacionConOptimo().valorPerdidoPorElGreedy());
        assertFalse(r.comparacionConOptimo().greedyEsOptimo());
    }

    @Test
    @DisplayName("El endpoint de contraejemplo documenta el caso donde el greedy no es óptimo")
    void contraejemplo() {
        PaquetesService.RespuestaContraejemplo r = paquetes.contraejemploGreedy();
        Greedy.Resultado greedy = r.greedy();
        assertEquals(160L, greedy.valorTotal());
        assertEquals(220L, r.optimo().valorOptimo());
        assertEquals(60L, r.valorPerdidoPorElGreedy());
        assertEquals(50, r.capacidadKg());
        assertEquals(3, r.juegoDeDatos().size());
    }

    @Test
    @DisplayName("Mochila vía servicio y validación de la capacidad")
    void mochila() {
        assertEquals(1960L, paquetes.mochila(40, false).valorOptimo());
        assertNotNull(paquetes.mochila(40, true).tablaDp());
        assertThrows(SolicitudInvalidaException.class, () -> paquetes.mochila(-1, false));
        assertThrows(SolicitudInvalidaException.class,
                () -> paquetes.mochila(PaquetesService.CAPACIDAD_MAXIMA_KG + 1, false));
    }

    // ------------------------------------------------------------ reparto

    @Test
    @DisplayName("Sin destinos explícitos el reparto usa los destinos de las cajas: 8 nodos, 1701 min")
    void repartoPorDefecto() {
        BranchAndBound.Resultado r = reparto.optimizar("CABA", null);
        assertEquals(8, r.destinos().size());
        assertEquals(1701L, r.costoTotalMinutos());
        assertTrue(r.nodosPodados() > 0);
    }

    @Test
    @DisplayName("El reparto valida destinos repetidos, inexistentes y la ausencia total de destinos")
    void repartoInvalido() {
        assertEquals(280L, reparto.optimizar("caba", List.of("quilmes", "la plata", "pilar")).costoTotalMinutos());
        assertThrows(SolicitudInvalidaException.class, () -> reparto.optimizar("CABA", List.of("PILAR", "pilar")));
        assertThrows(RecursoNoEncontradoException.class, () -> reparto.optimizar("CABA", List.of("MARTE")));

        RepartoService sinCajas = new RepartoService(grafoService,
                new RepositorioEnMemoria(DatosSemilla.nodos(), DatosSemilla.rutas(), List.of()));
        assertThrows(SolicitudInvalidaException.class, () -> sinCajas.optimizar("CABA", null));
    }
}
