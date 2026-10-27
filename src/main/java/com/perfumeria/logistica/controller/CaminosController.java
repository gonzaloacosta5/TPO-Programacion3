package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.algorithm.Dijkstra;
import com.perfumeria.logistica.algorithm.FloydWarshall;
import com.perfumeria.logistica.service.CaminosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caminos")
public class CaminosController {

    private final CaminosService servicio;

    public CaminosController(CaminosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/dijkstra")
    public Dijkstra.ResultadoCamino dijkstra(@RequestParam("origen") String origen,
                                             @RequestParam("destino") String destino) {
        return servicio.dijkstra(origen, destino);
    }

    @GetMapping("/floyd-warshall")
    public CaminosService.RespuestaTodosContraTodos floydWarshall() {
        return servicio.todosContraTodos();
    }

    @GetMapping("/floyd-warshall/camino")
    public CaminosService.RespuestaCaminoFloyd caminoFloyd(@RequestParam("origen") String origen,
                                                           @RequestParam("destino") String destino) {
        return servicio.caminoFloyd(origen, destino);
    }

    @GetMapping("/floyd-warshall/demo-ciclo-negativo")
    public FloydWarshall.Resultado demoCicloNegativo() {
        return servicio.demoCicloNegativo();
    }
}
