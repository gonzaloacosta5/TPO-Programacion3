package com.perfumeria.logistica.exception;

import com.perfumeria.logistica.algorithm.LimiteComputoExcedidoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

/**
 * Traduce toda excepción a una respuesta JSON manejada ({@link ErrorRespuesta}). Está prohibido devolver
 * un stack trace: los detalles de los errores inesperados se escriben solo en el log del servidor.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler({SolicitudInvalidaException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(RuntimeException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(LimiteComputoExcedidoException.class)
    public ResponseEntity<ErrorRespuesta> limiteExcedido(LimiteComputoExcedidoException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> parametroFaltante(MissingServletRequestParameterException ex,
                                                            HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoIncorrecto(MethodArgumentTypeMismatchException ex,
                                                         HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor con formato inválido", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException ex,
                                                            HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para este endpoint", req);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorRespuesta> rutaInexistente(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "El endpoint solicitado no existe", req);
    }

    @ExceptionHandler(BaseDeDatosNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> baseNoDisponible(BaseDeDatosNoDisponibleException ex,
                                                           HttpServletRequest req) {
        log.error("Neo4j no disponible al atender {}", req.getRequestURI(), ex);
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                "La base de datos no está disponible en este momento. Intentá de nuevo más tarde.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex, HttpServletRequest req) {
        // El detalle (con stack trace) queda en el log; al cliente solo le llega un mensaje genérico.
        log.error("Error inesperado al atender {}", req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intentá de nuevo más tarde.", req);
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje, HttpServletRequest req) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(Instant.now().toString(), estado.value(),
                estado.getReasonPhrase(), mensaje, req.getRequestURI());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
