package com.perfumeria.logistica.exception;

/** Neo4j no responde o rechazó la consulta. Se traduce a HTTP 503 (sin exponer detalles). */
public class BaseDeDatosNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BaseDeDatosNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
