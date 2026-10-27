package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.Kruskal;
import com.perfumeria.logistica.algorithm.Prim;
import com.perfumeria.logistica.algorithm.ResultadoMst;
import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import org.springframework.stereotype.Service;

@Service
public class ArbolExpansionService {

    private final GrafoService grafoService;

    public ArbolExpansionService(GrafoService grafoService) {
        this.grafoService = grafoService;
    }

    /** @param origen nodo inicial; si es null o vacío arranca desde el primer nodo (orden alfabético de ids) */
    public ResultadoMst prim(String origen) {
        GrafoMemoria g = grafoService.cargarGrafo();
        int inicio = (origen == null || origen.isBlank()) ? 0 : grafoService.resolverNodo(g, origen);
        return Prim.ejecutar(g, inicio);
    }

    public ResultadoMst kruskal() {
        return Kruskal.ejecutar(grafoService.cargarGrafo());
    }
}
