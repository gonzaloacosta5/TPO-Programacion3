package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.algorithm.Backtracking;
import com.perfumeria.logistica.service.CaminosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backtracking")
public class BacktrackingController {

    private final CaminosService servicio;

    public BacktrackingController(CaminosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/rutas")
    public Backtracking.Resultado rutas(
            @RequestParam("origen") String origen,
            @RequestParam("destino") String destino,
            @RequestParam(name = "maxMinutos", defaultValue = "800") long maxMinutos,
            @RequestParam(name = "maxSaltos", defaultValue = "6") int maxSaltos,
            @RequestParam(name = "limite", defaultValue = "20") int limite) {
        return servicio.rutasRestringidas(origen, destino, maxMinutos, maxSaltos, limite);
    }
}
