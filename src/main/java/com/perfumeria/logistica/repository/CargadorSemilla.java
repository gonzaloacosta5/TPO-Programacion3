package com.perfumeria.logistica.repository;

import com.perfumeria.logistica.exception.BaseDeDatosNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, si la base no tiene nodos, carga la red logística de {@link DatosSemilla}.
 * Se puede apagar con logistica.semilla.habilitada=false.
 */
@Component
@ConditionalOnProperty(name = "logistica.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class CargadorSemilla implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargadorSemilla.class);

    private final GrafoRepository repositorio;

    public CargadorSemilla(GrafoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (repositorio.contarNodos() > 0) {
                log.info("La base ya tiene datos: no se carga la semilla.");
                return;
            }
            repositorio.reemplazarGrafo(DatosSemilla.nodos(), DatosSemilla.rutas(), DatosSemilla.paquetes());
            log.info("Semilla cargada: {} nodos, {} rutas, {} paquetes.",
                    DatosSemilla.nodos().size(), DatosSemilla.rutas().size(), DatosSemilla.paquetes().size());
        } catch (BaseDeDatosNoDisponibleException e) {
            // La app sigue levantada: los endpoints responderán 503 manejado hasta que Neo4j esté disponible.
            log.error("No se pudo cargar la semilla en Neo4j: {} Causa: {}", e.getMessage(),
                    e.getCause() == null ? "desconocida" : e.getCause().getMessage());
        }
    }
}
