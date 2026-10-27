package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.algorithm.BranchAndBound;
import com.perfumeria.logistica.service.RepartoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reparto")
public class RepartoController {

    private final RepartoService servicio;

    public RepartoController(RepartoService servicio) {
        this.servicio = servicio;
    }

    /** destinos admite lista separada por comas (destinos=QUILMES,PILAR) o el parámetro repetido. */
    @GetMapping("/branch-and-bound")
    public BranchAndBound.Resultado ramificacionYPoda(
            @RequestParam(name = "origen", defaultValue = "CABA") String origen,
            @RequestParam(name = "destinos", required = false) List<String> destinos) {
        return servicio.optimizar(origen, destinos);
    }
}
