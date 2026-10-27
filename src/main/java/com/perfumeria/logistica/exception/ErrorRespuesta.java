package com.perfumeria.logistica.exception;

/**
 * Cuerpo estándar de todos los errores de la API. Nunca incluye stack trace, nombres de clases
 * ni mensajes internos de excepciones inesperadas.
 */
public record ErrorRespuesta(String timestamp, int estado, String error, String mensaje, String ruta) {
}
