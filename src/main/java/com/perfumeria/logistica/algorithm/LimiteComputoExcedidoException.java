package com.perfumeria.logistica.algorithm;

/**
 * Un algoritmo de búsqueda exhaustiva (backtracking, branch and bound) superó el tope de nodos
 * que se le permite explorar en una solicitud. Evita que una consulta exponencial deje colgado el servidor.
 */
public class LimiteComputoExcedidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LimiteComputoExcedidoException(String mensaje) {
        super(mensaje);
    }
}
