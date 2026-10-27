package com.perfumeria.logistica.repository;

import com.perfumeria.logistica.exception.BaseDeDatosNoDisponibleException;
import com.perfumeria.logistica.model.Nodo;
import com.perfumeria.logistica.model.Paquete;
import com.perfumeria.logistica.model.Ruta;
import com.perfumeria.logistica.model.TipoNodo;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementación sobre el driver oficial de Neo4j (el bean {@link Driver} lo crea
 * Spring Boot a partir de spring.neo4j.*).
 *
 * <p>Modelo en el grafo:
 * <pre>
 * (:Nodo {id, nombre, tipo})-[:RUTA {minutos}]->(:Nodo)         una sola relación por ruta física
 * (:Paquete {id, descripcion, pesoKg, valor})-[:SE_ENTREGA_EN]->(:Nodo)
 * </pre>
 * Las rutas se leen siempre ordenadas por (origen, destino) para que el resultado sea determinista.</p>
 */
@Repository
public class Neo4jGrafoRepository implements GrafoRepository {

    private static final String MSG_BD = "La base de datos no está disponible o rechazó la consulta.";

    private final Driver driver;

    public Neo4jGrafoRepository(Driver driver) {
        this.driver = driver;
    }

    @Override
    public List<Nodo> obtenerNodos() {
        return leer("MATCH (n:Nodo) RETURN n.id AS id, n.nombre AS nombre, n.tipo AS tipo ORDER BY n.id",
                r -> new Nodo(
                        r.get("id").asString(),
                        r.get("nombre").asString(),
                        TipoNodo.valueOf(r.get("tipo").asString())));
    }

    @Override
    public List<Ruta> obtenerRutas() {
        return leer("MATCH (a:Nodo)-[r:RUTA]->(b:Nodo) "
                        + "RETURN a.id AS origen, b.id AS destino, r.minutos AS minutos ORDER BY a.id, b.id",
                r -> new Ruta(
                        r.get("origen").asString(),
                        r.get("destino").asString(),
                        r.get("minutos").asInt()));
    }

    @Override
    public List<Paquete> obtenerPaquetes() {
        return leer("MATCH (p:Paquete) OPTIONAL MATCH (p)-[:SE_ENTREGA_EN]->(d:Nodo) "
                        + "RETURN p.id AS id, p.descripcion AS descripcion, p.pesoKg AS pesoKg, "
                        + "p.valor AS valor, d.id AS destino ORDER BY p.id",
                r -> new Paquete(
                        r.get("id").asString(),
                        r.get("descripcion").asString(),
                        r.get("pesoKg").asInt(),
                        r.get("valor").asInt(),
                        r.get("destino").isNull() ? null : r.get("destino").asString()));
    }

    @Override
    public long contarNodos() {
        List<Long> resultado = leer("MATCH (n:Nodo) RETURN count(n) AS total", r -> r.get("total").asLong());
        return resultado.isEmpty() ? 0L : resultado.get(0);
    }

    @Override
    public void reemplazarGrafo(List<Nodo> nodos, List<Ruta> rutas, List<Paquete> paquetes) {
        List<Map<String, Object>> nodosParam = new ArrayList<>();
        for (Nodo n : nodos) {
            nodosParam.add(Map.of("id", n.id(), "nombre", n.nombre(), "tipo", n.tipo().name()));
        }
        List<Map<String, Object>> rutasParam = new ArrayList<>();
        for (Ruta r : rutas) {
            rutasParam.add(Map.of("origen", r.origenId(), "destino", r.destinoId(), "minutos", r.minutos()));
        }
        List<Map<String, Object>> paquetesParam = new ArrayList<>();
        for (Paquete p : paquetes) {
            paquetesParam.add(Map.of(
                    "id", p.id(), "descripcion", p.descripcion(),
                    "pesoKg", p.pesoKg(), "valor", p.valor(), "destino", p.destinoId()));
        }

        try (Session session = driver.session()) {
            // Las operaciones de esquema van en transacciones propias (Neo4j no las mezcla con escrituras).
            session.run("CREATE CONSTRAINT nodo_id IF NOT EXISTS FOR (n:Nodo) REQUIRE n.id IS UNIQUE").consume();
            session.run("CREATE CONSTRAINT paquete_id IF NOT EXISTS FOR (p:Paquete) REQUIRE p.id IS UNIQUE").consume();

            session.executeWrite(tx -> {
                tx.run("MATCH (n) WHERE n:Nodo OR n:Paquete DETACH DELETE n").consume();
                tx.run("UNWIND $nodos AS n CREATE (:Nodo {id: n.id, nombre: n.nombre, tipo: n.tipo})",
                        Map.of("nodos", nodosParam)).consume();
                tx.run("UNWIND $rutas AS r "
                                + "MATCH (a:Nodo {id: r.origen}), (b:Nodo {id: r.destino}) "
                                + "CREATE (a)-[:RUTA {minutos: r.minutos}]->(b)",
                        Map.of("rutas", rutasParam)).consume();
                tx.run("UNWIND $paquetes AS p "
                                + "MATCH (d:Nodo {id: p.destino}) "
                                + "CREATE (:Paquete {id: p.id, descripcion: p.descripcion, pesoKg: p.pesoKg, valor: p.valor})"
                                + "-[:SE_ENTREGA_EN]->(d)",
                        Map.of("paquetes", paquetesParam)).consume();
                return null;
            });
        } catch (Neo4jException e) {
            throw new BaseDeDatosNoDisponibleException(MSG_BD, e);
        }
    }

    private <T> List<T> leer(String cypher, Function<Record, T> mapeador) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run(cypher).list(mapeador));
        } catch (Neo4jException e) {
            throw new BaseDeDatosNoDisponibleException(MSG_BD, e);
        }
    }
}
