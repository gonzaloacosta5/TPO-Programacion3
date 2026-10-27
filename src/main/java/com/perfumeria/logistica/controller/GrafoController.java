package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.model.Ruta;
import com.perfumeria.logistica.service.GrafoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de los datos cargados en Neo4j. No contiene lógica: delega todo en el servicio. */
@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    private final GrafoService servicio;

    public GrafoController(GrafoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    public GrafoService.ResumenGrafo resumen() {
        return servicio.resumen();
    }

    @GetMapping("/nodos")
    public List<Nodo> nodos() {
        return servicio.nodos();
    }

    @GetMapping("/rutas")
    public List<Ruta> rutas() {
        return servicio.rutas();
    }

    @GetMapping("/paquetes")
    public List<Paquete> paquetes() {
        return servicio.paquetes();
    }
}
