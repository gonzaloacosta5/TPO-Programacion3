package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.algorithm.ResultadoMst;
import com.perfumeria.logistica.service.ArbolExpansionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mst")
public class ArbolExpansionController {

    private final ArbolExpansionService servicio;

    public ArbolExpansionController(ArbolExpansionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/prim")
    public ResultadoMst prim(@RequestParam(name = "origen", required = false) String origen) {
        return servicio.prim(origen);
    }

    @GetMapping("/kruskal")
    public ResultadoMst kruskal() {
        return servicio.kruskal();
    }
}
