package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.algorithm.Recorridos;
import com.perfumeria.logistica.service.RecorridoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recorridos")
public class RecorridosController {

    private final RecorridoService servicio;

    public RecorridosController(RecorridoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/bfs")
    public Recorridos.ResultadoBfs bfs(@RequestParam("origen") String origen) {
        return servicio.bfs(origen);
    }

    @GetMapping("/dfs")
    public Recorridos.ResultadoDfs dfs(@RequestParam("origen") String origen) {
        return servicio.dfs(origen);
    }
}
