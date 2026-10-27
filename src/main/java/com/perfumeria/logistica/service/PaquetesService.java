package com.perfumeria.logistica.service;

import com.perfumeria.logistica.algorithm.Greedy;
import com.perfumeria.logistica.algorithm.MochilaDP;
import com.perfumeria.logistica.algorithm.ordenamiento.Ordenamiento;
import com.perfumeria.logistica.exception.SolicitudInvalidaException;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.repository.GrafoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Ordenamiento de cajas y carga de la camioneta (greedy y mochila 0/1). */
@Service
public class PaquetesService {

    public static final int CAPACIDAD_MAXIMA_KG = 100_000;

    private final GrafoRepository repositorio;

    public PaquetesService(GrafoRepository repositorio) {
        this.repositorio = repositorio;
    }

    // ---------------------------------------------------------------- ordenamiento

    public record RespuestaOrdenamiento(String algoritmo, String criterio, String orden, long comparaciones,
                                        List<Paquete> paquetes) {
    }

    public RespuestaOrdenamiento ordenarConQuickSort(String criterio, String orden) {
        return ordenar(true, criterio, orden);
    }

    public RespuestaOrdenamiento ordenarConMergeSort(String criterio, String orden) {
        return ordenar(false, criterio, orden);
    }

    private RespuestaOrdenamiento ordenar(boolean quick, String criterio, String orden) {
        String criterioNormalizado = criterio == null ? "valor" : criterio.trim().toLowerCase(Locale.ROOT);
        String ordenNormalizado = orden == null ? "asc" : orden.trim().toLowerCase(Locale.ROOT);
        if (!ordenNormalizado.equals("asc") && !ordenNormalizado.equals("desc")) {
            throw new SolicitudInvalidaException("orden debe ser 'asc' o 'desc'");
        }
        Comparator<Paquete> base = comparadorBase(criterioNormalizado);
        Comparator<Paquete> direccional = ordenNormalizado.equals("desc") ? base.reversed() : base;
        // Desempate por id: orden total, así QuickSort (inestable) y MergeSort dan el mismo resultado.
        Comparator<Paquete> completo = direccional.thenComparing(Paquete::id);

        long[] contador = {0};
        Comparator<Paquete> contando = (a, b) -> {
            contador[0]++;
            return completo.compare(a, b);
        };

        Paquete[] arreglo = repositorio.obtenerPaquetes().toArray(new Paquete[0]);
        if (quick) {
            Ordenamiento.quickSort(arreglo, contando);
        } else {
            Ordenamiento.mergeSort(arreglo, contando);
        }
        return new RespuestaOrdenamiento(quick ? "QuickSort" : "MergeSort", criterioNormalizado, ordenNormalizado,
                contador[0], List.of(arreglo));
    }

    private static Comparator<Paquete> comparadorBase(String criterio) {
        return switch (criterio) {
            case "valor" -> Comparator.comparingInt(Paquete::valor);
            case "peso" -> Comparator.comparingInt(Paquete::pesoKg);
            case "ratio" -> (a, b) -> Long.compare((long) a.valor() * b.pesoKg(), (long) b.valor() * a.pesoKg());
            case "id" -> Comparator.comparing(Paquete::id);
            default -> throw new SolicitudInvalidaException(
                    "criterio inválido '" + criterio + "'. Valores posibles: valor, peso, ratio, id");
        };
    }

    // ---------------------------------------------------------------- greedy

    public record ComparacionConOptimo(long valorOptimoMochila, long valorPerdidoPorElGreedy, boolean greedyEsOptimo) {
    }

    public record RespuestaGreedy(Greedy.Resultado greedy, ComparacionConOptimo comparacionConOptimo) {
    }

    public record RespuestaContraejemplo(String explicacion, int capacidadKg, List<Paquete> juegoDeDatos,
                                         Greedy.Resultado greedy, MochilaDP.Resultado optimo,
                                         long valorPerdidoPorElGreedy) {
    }

    public RespuestaGreedy greedy(int capacidadKg) {
        validarCapacidad(capacidadKg);
        List<Paquete> paquetes = repositorio.obtenerPaquetes();
        Greedy.Resultado greedy = Greedy.seleccionarPorRatio(paquetes, capacidadKg);
        long optimo = MochilaDP.resolver(paquetes, capacidadKg, false).valorOptimo();
        return new RespuestaGreedy(greedy,
                new ComparacionConOptimo(optimo, optimo - greedy.valorTotal(), optimo == greedy.valorTotal()));
    }

    /**
     * Juego de datos clásico donde el greedy por ratio falla (capacidad 50 kg): la caja A tiene el mejor
     * ratio (6 USD/kg) y el greedy la toma, pero eso deja 20 kg de hueco en los que solo entra B; el
     * óptimo se salta A y carga B + C aprovechando los 50 kg completos.
     */
    public RespuestaContraejemplo contraejemploGreedy() {
        int capacidad = 50;
        List<Paquete> juego = List.of(
                new Paquete("CE-A", "Caja A: 10 kg, 60 USD (ratio 6,0)", 10, 60, null),
                new Paquete("CE-B", "Caja B: 20 kg, 100 USD (ratio 5,0)", 20, 100, null),
                new Paquete("CE-C", "Caja C: 30 kg, 120 USD (ratio 4,0)", 30, 120, null));
        Greedy.Resultado greedy = Greedy.seleccionarPorRatio(juego, capacidad);
        MochilaDP.Resultado optimo = MochilaDP.resolver(juego, capacidad, true);
        return new RespuestaContraejemplo(
                "El greedy toma A y B (30 kg, 160 USD) y C ya no entra en los 20 kg que quedan. "
                        + "El óptimo es B + C (50 kg, 220 USD): el greedy pierde 60 USD.",
                capacidad, juego, greedy, optimo, optimo.valorOptimo() - greedy.valorTotal());
    }

    // ---------------------------------------------------------------- mochila

    public MochilaDP.Resultado mochila(int capacidadKg, boolean incluirTabla) {
        validarCapacidad(capacidadKg);
        return MochilaDP.resolver(repositorio.obtenerPaquetes(), capacidadKg, incluirTabla);
    }

    private static void validarCapacidad(int capacidadKg) {
        if (capacidadKg < 0 || capacidadKg > CAPACIDAD_MAXIMA_KG) {
            throw new SolicitudInvalidaException("capacidadKg debe estar entre 0 y " + CAPACIDAD_MAXIMA_KG);
        }
    }
}
