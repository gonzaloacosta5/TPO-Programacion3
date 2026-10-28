package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.BranchAndBound;
import com.perfumeria.logistica.algorithm.BranchAndBoundCarga;
import com.perfumeria.logistica.algorithm.estructura.GrafoMemoria;
import com.perfumeria.logistica.exception.SolicitudInvalidaException;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.GrafoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RepartoService {

    private final GrafoService grafoService;
    private final GrafoRepository repositorio;

    public RepartoService(GrafoService grafoService, GrafoRepository repositorio) {
        this.grafoService = grafoService;
        this.repositorio = repositorio;
    }

    /**
     * @param origen   depósito de salida (y de regreso)
     * @param destinos nodos donde hay que entregar; si es null o vacío se usan los destinos de todos los
     *                 paquetes cargados (sin repetir y sin el propio origen)
     */
    public BranchAndBound.Resultado optimizar(String origen, List<String> destinos) {
        GrafoMemoria g = grafoService.cargarGrafo();
        int o = grafoService.resolverNodo(g, origen);

        Set<Integer> indices = new LinkedHashSet<>();
        if (destinos == null || destinos.isEmpty()) {
            for (Paquete p : repositorio.obtenerPaquetes()) {
                if (p.destinoId() != null) {
                    int d = g.indice(p.destinoId());
                    if (d >= 0 && d != o) {
                        indices.add(d);
                    }
                }
            }
        } else {
            for (String destino : destinos) {
                int d = grafoService.resolverNodo(g, destino);
                if (!indices.add(d)) {
                    throw new SolicitudInvalidaException("Destino repetido: " + destino);
                }
            }
        }
        if (indices.isEmpty()) {
            throw new SolicitudInvalidaException("No hay destinos para repartir");
        }

        int[] arreglo = new int[indices.size()];
        int i = 0;
        for (int d : indices) {
            arreglo[i++] = d;
        }
        return BranchAndBound.optimizarReparto(g, o, arreglo, true);
    }

    /** Reparte las cajas cargadas entre {@code camionetas} camionetas iguales, minimizando la más cargada. */
    public BranchAndBoundCarga.Resultado repartirCajas(int camionetas) {
        if (camionetas < 1 || camionetas > BranchAndBoundCarga.MAX_CAMIONETAS) {
            throw new SolicitudInvalidaException(
                    "camionetas debe estar entre 1 y " + BranchAndBoundCarga.MAX_CAMIONETAS);
        }
        List<Paquete> cajas = repositorio.obtenerPaquetes();
        if (cajas.isEmpty()) {
            throw new SolicitudInvalidaException("No hay cajas cargadas para repartir");
        }
        return BranchAndBoundCarga.repartir(cajas, camionetas, true);
    }
}
