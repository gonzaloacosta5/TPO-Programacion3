package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.Recorridos;
import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import org.springframework.stereotype.Service;

@Service
public class RecorridoService {

    private final GrafoService grafoService;

    public RecorridoService(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    public Recorridos.ResultadoBfs bfs(String origen) {
        GrafoMemoria g = grafoService.cargarGrafo();
        return Recorridos.bfs(g, grafoService.resolverNodo(g, origen));
    }

    public Recorridos.ResultadoDfs dfs(String origen) {
        GrafoMemoria g = grafoService.cargarGrafo();
        return Recorridos.dfs(g, grafoService.resolverNodo(g, origen));
    }
}
