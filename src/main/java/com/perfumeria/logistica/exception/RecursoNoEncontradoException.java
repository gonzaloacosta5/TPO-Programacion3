package com.perfumeria.logistica.exception;

/** El recurso pedido (nodo, ruta, etc.) no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
