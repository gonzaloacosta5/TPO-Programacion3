package com.perfumeria.logistica.exception;

/** Parámetros inválidos o inconsistentes en la solicitud. Se traduce a HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
