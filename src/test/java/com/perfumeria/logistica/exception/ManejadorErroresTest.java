package com.perfumeria.logistica.exception;

import com.perfumeria.logistica.algorithm.LimiteComputoExcedidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el {@link ManejadorErrores} traduzca cada excepción a un JSON manejado y que NUNCA
 * filtre un stack trace ni detalles internos. Usa un controller de mentira que solo lanza excepciones.
 */
class ManejadorErroresTest {

    @RestController
    public static class ControladorQueFalla {

        @GetMapping("/falla/no-encontrado")
        public String noEncontrado() {
            throw new RecursoNoEncontradoException("El nodo 'X' no existe");
        }

        @GetMapping("/falla/invalida")
        public String invalida() {
            throw new SolicitudInvalidaException("capacidadKg debe estar entre 0 y 100000");
        }

        @GetMapping("/falla/argumento")
        public String argumento() {
            throw new IllegalArgumentException("Hay destinos repetidos: PILAR");
        }

        @GetMapping("/falla/limite")
        public String limite() {
            throw new LimiteComputoExcedidoException("El backtracking superó el límite de nodos");
        }

        @GetMapping("/falla/base")
        public String base() {
            throw new BaseDeDatosNoDisponibleException("La base de datos no está disponible",
                    new RuntimeException("Connection refused: bolt://10.0.0.5:7687 (usuario neo4j)"));
        }

        @GetMapping("/falla/inesperada")
        public String inesperada() {
            throw new IllegalStateException("detalle interno secreto en com.perfumeria.logistica.service.Algo");
        }

        @GetMapping("/falla/parametro")
        public String parametro(@RequestParam("n") int n) {
            return "n=" + n;
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorQueFalla())
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @Test
    @DisplayName("Recurso inexistente -> 404 con el mensaje y la ruta")
    void noEncontrado() throws Exception {
        mvc.perform(get("/falla/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("El nodo 'X' no existe"))
                .andExpect(jsonPath("$.ruta").value("/falla/no-encontrado"));
    }

    @Test
    @DisplayName("Solicitud inválida, IllegalArgumentException y parámetros mal formados -> 400")
    void solicitudInvalida() throws Exception {
        mvc.perform(get("/falla/invalida")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("capacidadKg debe estar entre 0 y 100000"));
        mvc.perform(get("/falla/argumento")).andExpect(status().isBadRequest());
        mvc.perform(get("/falla/parametro")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro obligatorio 'n'"));
        mvc.perform(get("/falla/parametro").param("n", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parámetro 'n' tiene un valor con formato inválido"));
    }

    @Test
    @DisplayName("Límite de cómputo excedido -> 422")
    void limiteExcedido() throws Exception {
        mvc.perform(get("/falla/limite")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Método HTTP no soportado -> 405 manejado")
    void metodoNoPermitido() throws Exception {
        mvc.perform(post("/falla/no-encontrado")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.estado").value(405));
    }

    @Test
    @DisplayName("Base de datos caída -> 503 sin revelar host, puerto ni usuario")
    void baseNoDisponible() throws Exception {
        String cuerpo = mvc.perform(get("/falla/base"))
                .andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();
        assertFalse(cuerpo.contains("10.0.0.5"), "no debe filtrar la dirección de la base");
        assertFalse(cuerpo.contains("bolt://"));
        assertFalse(cuerpo.contains("neo4j"));
    }

    @Test
    @DisplayName("Un error inesperado -> 500 genérico, sin stack trace, sin nombres de clase y sin el mensaje interno")
    void errorInesperadoNoFiltraNada() throws Exception {
        String cuerpo = mvc.perform(get("/falla/inesperada"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andReturn().getResponse().getContentAsString();

        assertFalse(cuerpo.contains("secreto"), "no debe filtrar el mensaje de la excepción");
        assertFalse(cuerpo.contains("IllegalStateException"));
        assertFalse(cuerpo.contains("com.perfumeria"));
        assertFalse(cuerpo.contains("stackTrace"));
        assertFalse(cuerpo.contains("trace"));
        assertFalse(cuerpo.contains("\tat "));
        assertFalse(cuerpo.contains(".java"));
    }
}
