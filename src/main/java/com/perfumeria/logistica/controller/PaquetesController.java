package com.perfumeria.logistica.controller;

import com.perfumeria.logistica.algorithm.MochilaDP;
import com.perfumeria.logistica.service.PaquetesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paquetes")
public class PaquetesController {

    private final PaquetesService servicio;

    public PaquetesController(PaquetesService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/ordenar/quicksort")
    public PaquetesService.RespuestaOrdenamiento quickSort(
            @RequestParam(name = "criterio", defaultValue = "valor") String criterio,
            @RequestParam(name = "orden", defaultValue = "asc") String orden) {
        return servicio.ordenarConQuickSort(criterio, orden);
    }

    @GetMapping("/ordenar/mergesort")
    public PaquetesService.RespuestaOrdenamiento mergeSort(
            @RequestParam(name = "criterio", defaultValue = "valor") String criterio,
            @RequestParam(name = "orden", defaultValue = "asc") String orden) {
        return servicio.ordenarConMergeSort(criterio, orden);
    }

    @GetMapping("/greedy")
    public PaquetesService.RespuestaGreedy greedy(
            @RequestParam(name = "capacidadKg", defaultValue = "40") int capacidadKg) {
        return servicio.greedy(capacidadKg);
    }

    @GetMapping("/greedy/contraejemplo")
    public PaquetesService.RespuestaContraejemplo contraejemploGreedy() {
        return servicio.contraejemploGreedy();
    }

    @GetMapping("/mochila")
    public MochilaDP.Resultado mochila(
            @RequestParam(name = "capacidadKg", defaultValue = "40") int capacidadKg,
            @RequestParam(name = "incluirTabla", defaultValue = "false") boolean incluirTabla) {
        return servicio.mochila(capacidadKg, incluirTabla);
    }
}
