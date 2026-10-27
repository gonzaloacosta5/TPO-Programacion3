# Endpoints y fichas de algoritmos

Red logística de un e-commerce de perfumería. Todos los ejemplos de respuesta de este documento son la
salida **real** de los servicios sobre los datos semilla (12 nodos, 17 rutas, 8 cajas); donde una respuesta es
muy larga se indica "(abreviado)".

**Convenciones**

- El grafo es **no dirigido**. Un nodo es un depósito, un centro de distribución o un punto de retiro; una arista es una
  ruta terrestre; **el costo de una arista es el tiempo de viaje en minutos**.
- Los ids de nodo van en MAYÚSCULAS con guion bajo (`LA_PLATA`). La API los acepta en cualquier capitalización, con
  espacios y con tildes (`la plata`, `Zárate`).
- **V** = cantidad de vértices (12 en la semilla), **E** = cantidad de aristas (17).
- Todo algoritmo trabaja sobre un grafo **ya cargado en memoria**: el servicio lee Neo4j una vez por solicitud y los
  algoritmos nunca consultan la base.
- Toda la lógica está en el paquete `algorithm`; los controllers solo reciben parámetros y llaman a un servicio.

## Índice

| # | Algoritmo | Endpoint | Hito | Clase |
|---|-----------|----------|------|-------|
| 0 | Datos del grafo | `GET /api/grafo/resumen`, `/nodos`, `/rutas`, `/paquetes` | 1 | `GrafoService` |
| 1 | BFS | `GET /api/recorridos/bfs` | 4 | `Recorridos` |
| 2 | DFS | `GET /api/recorridos/dfs` | 4 | `Recorridos` |
| 3 | Dijkstra | `GET /api/caminos/dijkstra` | 5 | `Dijkstra` + `MinHeap` |
| 4 | Prim | `GET /api/mst/prim` | 5 | `Prim` + `MinHeap` |
| 5 | Kruskal + Union-Find | `GET /api/mst/kruskal` | 5 | `Kruskal` + `UnionFind` |
| 6 | QuickSort | `GET /api/paquetes/ordenar/quicksort` | 2 | `Ordenamiento` |
| 7 | MergeSort | `GET /api/paquetes/ordenar/mergesort` | 2 | `Ordenamiento` |
| 8 | Greedy | `GET /api/paquetes/greedy`, `/greedy/contraejemplo` | 3 y 6 | `Greedy` |
| 9 | Mochila 0/1 (PD) | `GET /api/paquetes/mochila` | 6 | `MochilaDP` |
| 10 | Floyd-Warshall | `GET /api/caminos/floyd-warshall`, `/camino`, `/demo-ciclo-negativo` | 7 | `FloydWarshall` |
| 11 | Backtracking | `GET /api/backtracking/rutas` | 8 | `Backtracking` |
| 12 | Branch & Bound | `GET /api/reparto/branch-and-bound` | 11 | `BranchAndBound` |

---

## 0. Datos del grafo

### `GET /api/grafo/resumen`

Cantidad de vértices, aristas y paquetes cargados en Neo4j. Es el chequeo de que el entorno está funcionando.

```bash
curl "http://localhost:8080/api/grafo/resumen"
```

```json
{
  "vertices": 12,
  "aristas": 17,
  "paquetes": 8,
  "nodosPorTipo": { "DEPOSITO": 2, "CENTRO_DISTRIBUCION": 5, "PUNTO_RETIRO": 5 },
  "unidadCosto": "minutos",
  "dirigido": false
}
```

`GET /api/grafo/nodos`, `/rutas` y `/paquetes` devuelven las listas crudas (por ejemplo, una ruta es
`{"origenId": "CABA", "destinoId": "LOMAS_DE_ZAMORA", "minutos": 32}` y un paquete
`{"id": "PQ-01", "descripcion": "Caja Perfumes de Nicho x4", "pesoKg": 8, "valor": 520, "destinoId": "QUILMES"}`).

---

## 1. BFS — recorrido en anchura

### `GET /api/recorridos/bfs?origen=CABA`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `origen` | sí | Nodo desde el que se recorre |

Visita los nodos por niveles (primero todos los que están a 1 salto, después a 2, etc.). Los vecinos se recorren
en orden alfabético, así que el resultado es determinista. Informa los **saltos mínimos** desde el origen y el árbol
de padres.

```bash
curl "http://localhost:8080/api/recorridos/bfs?origen=CABA"
```

```json
{
  "algoritmo": "BFS",
  "origen": "CABA",
  "ordenVisita": ["CABA", "CAMPANA", "LOMAS_DE_ZAMORA", "PILAR", "QUILMES", "ROSARIO",
                  "ZARATE", "LA_PLATA", "PARANA", "SANTA_FE", "CORRIENTES", "RESISTENCIA"],
  "saltosDesdeOrigen": { "CABA": 0, "CAMPANA": 1, "LOMAS_DE_ZAMORA": 1, "PILAR": 1, "QUILMES": 1, "ROSARIO": 1,
                         "ZARATE": 2, "LA_PLATA": 2, "PARANA": 2, "SANTA_FE": 2, "CORRIENTES": 3, "RESISTENCIA": 3 },
  "padres": { "CAMPANA": "CABA", "ZARATE": "CAMPANA", "LA_PLATA": "LOMAS_DE_ZAMORA", "PARANA": "ROSARIO",
              "CORRIENTES": "PARANA", "RESISTENCIA": "SANTA_FE" },
  "nodosVisitados": 12,
  "nodosNoAlcanzados": 0
}
```

(`padres` abreviado: la respuesta real lista los 11 nodos distintos del origen.)

**Complejidad**

- **Tiempo O(V + E).** Cada vértice se marca como visitado al encolarse y por eso entra a la cola una sola vez: V
  operaciones. Al desencolar un vértice se recorre su lista de adyacencia completa, y la suma de los grados de todos
  los vértices es 2E: O(E). Total O(V + E).
- **Espacio O(V) extra.** Cola (arreglo de tamaño V), `visitado`, `padre` y `saltos`, todos de tamaño V. (El grafo en
  sí ocupa O(V + E) y no se cuenta como espacio auxiliar.)
- **Estructura:** cola FIFO implementada con un arreglo de tamaño V y dos índices (cabeza y fin); no hace falta
  más porque cada vértice se encola una sola vez.

---

## 2. DFS — recorrido en profundidad

### `GET /api/recorridos/dfs?origen=CABA`

Mismos parámetros que BFS. Devuelve el orden de descubrimiento (preorden), el orden de finalización (postorden)
y el árbol de padres.

```bash
curl "http://localhost:8080/api/recorridos/dfs?origen=CABA"
```

```json
{
  "algoritmo": "DFS",
  "origen": "CABA",
  "ordenVisita": ["CABA", "CAMPANA", "PILAR", "ZARATE", "ROSARIO", "PARANA", "CORRIENTES",
                  "RESISTENCIA", "SANTA_FE", "LOMAS_DE_ZAMORA", "LA_PLATA", "QUILMES"],
  "ordenFinalizacion": ["PILAR", "SANTA_FE", "RESISTENCIA", "CORRIENTES", "PARANA", "ROSARIO",
                        "ZARATE", "CAMPANA", "QUILMES", "LA_PLATA", "LOMAS_DE_ZAMORA", "CABA"],
  "padres": { "CAMPANA": "CABA", "PILAR": "CAMPANA", "ZARATE": "CAMPANA", "ROSARIO": "ZARATE", "...": "..." },
  "nodosVisitados": 12,
  "nodosNoAlcanzados": 0
}
```

**Complejidad**

- **Tiempo O(V + E).** Cada vértice se visita una vez (se marca al entrar) y cada lista de adyacencia se recorre una vez.
- **Espacio O(V).** `visitado`, `padre` y las listas de pre/postorden son O(V); la **pila de recursión** llega a
  profundidad V en el peor caso (un grafo que es un camino). Con grafos de cientos de miles de nodos habría que
  pasar a una versión iterativa con pila explícita.
- **Estructura:** la pila de llamadas del lenguaje.

---

## 3. Dijkstra — camino mínimo con reconstrucción

### `GET /api/caminos/dijkstra?origen=CABA&destino=CORRIENTES`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `origen` | sí | Nodo de salida |
| `destino` | sí | Nodo de llegada |

Camino de menor tiempo entre dos nodos. Corta apenas el destino queda finalizado. Devuelve el camino, cada tramo con
sus minutos, y cuántos nodos expandió y aristas examinó. Si el destino no es alcanzable responde 200 con
`"alcanzable": false`.

```bash
curl "http://localhost:8080/api/caminos/dijkstra?origen=CABA&destino=CORRIENTES"
```

```json
{
  "origen": "CABA",
  "destino": "CORRIENTES",
  "alcanzable": true,
  "costoMinutos": 747,
  "saltos": 4,
  "camino": ["CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES"],
  "tramos": [
    { "desde": "CABA", "hasta": "ROSARIO", "minutos": 200 },
    { "desde": "ROSARIO", "hasta": "SANTA_FE", "minutos": 105 },
    { "desde": "SANTA_FE", "hasta": "RESISTENCIA", "minutos": 420 },
    { "desde": "RESISTENCIA", "hasta": "CORRIENTES", "minutos": 22 }
  ],
  "nodosExpandidos": 12,
  "aristasExaminadas": 32
}
```

**Verificación a mano:** 200 + 105 + 420 + 22 = 747. La alternativa por Paraná (CABA→ROSARIO→SANTA_FE→PARANA→CORRIENTES)
cuesta 200 + 105 + 28 + 450 = 783, y la que rodea por Campana y Zárate (CABA→CAMPANA→ZARATE→ROSARIO…) suma 20 minutos más
que ir directo a Rosario, por eso Dijkstra descarta ambas.

**Complejidad**

- **Tiempo O((V + E) log V).** Cada arco se examina una vez al expandir su origen: O(E). Cada relajación exitosa
  inserta una entrada en el heap, así que el heap tiene como máximo O(E) entradas y cada inserción/extracción cuesta
  O(log E) = O(log V) porque E ≤ V² (log E ≤ 2 log V). Cada vértice se expande una sola vez (las entradas obsoletas se
  descartan al extraerlas con el arreglo `cerrado`). Total: O(E log V + V log V).
- **Espacio O(V + E).** Arreglos `dist`, `padre`, `cerrado` de tamaño V más el heap, que con "lazy deletion" puede
  contener hasta O(E) entradas.
- **Estructura que sostiene el orden:** `MinHeap` propio (heap binario). Con búsqueda lineal del mínimo en lugar del
  heap el algoritmo sería O(V²), no O((V + E) log V).
- **Restricción:** pesos no negativos. Con un peso negativo la API responde 400 y sugiere Floyd-Warshall.

---

## 4. Prim — árbol de expansión mínima

### `GET /api/mst/prim?origen=CABA`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `origen` | no | Nodo desde el que crece el árbol (por defecto el primero alfabéticamente, `CABA`) |

Conecta todos los nodos con el menor tiempo de ruta total (por ejemplo, para decidir qué tramos de una red troncal
mantener). Si el grafo no es conexo solo cubre la componente del origen y devuelve `"conexo": false`.

```bash
curl "http://localhost:8080/api/mst/prim?origen=CABA"
```

```json
{
  "algoritmo": "Prim",
  "origen": "CABA",
  "costoTotalMinutos": 907,
  "aristasEnArbol": 11,
  "nodosCubiertos": 12,
  "conexo": true,
  "aristas": [
    { "desde": "CABA", "hasta": "LOMAS_DE_ZAMORA", "minutos": 32 },
    { "desde": "LOMAS_DE_ZAMORA", "hasta": "QUILMES", "minutos": 25 },
    { "desde": "QUILMES", "hasta": "LA_PLATA", "minutos": 45 },
    { "desde": "CABA", "hasta": "PILAR", "minutos": 55 },
    { "desde": "PILAR", "hasta": "CAMPANA", "minutos": 35 },
    { "desde": "CAMPANA", "hasta": "ZARATE", "minutos": 20 },
    { "desde": "ZARATE", "hasta": "ROSARIO", "minutos": 120 },
    { "desde": "ROSARIO", "hasta": "SANTA_FE", "minutos": 105 },
    { "desde": "SANTA_FE", "hasta": "PARANA", "minutos": 28 },
    { "desde": "SANTA_FE", "hasta": "RESISTENCIA", "minutos": 420 },
    { "desde": "RESISTENCIA", "hasta": "CORRIENTES", "minutos": 22 }
  ]
}
```

**Complejidad**

- **Tiempo O(E log V).** Cada arco se examina una vez y, si mejora la clave de su extremo, inserta una entrada en el
  heap (O(E) entradas, O(log V) cada operación). Los V vértices se agregan al árbol una sola vez.
- **Espacio O(V + E).** Arreglos `clave`, `padre`, `enArbol` (V) y el heap (hasta O(E) entradas con lazy deletion).
- **Estructura:** `MinHeap` propio. (Con un arreglo de claves y búsqueda lineal sería O(V²).)

---

## 5. Kruskal — árbol de expansión mínima con Union-Find

### `GET /api/mst/kruskal`

Sin parámetros. Ordena todas las aristas por peso y las agrega en ese orden si no cierran un ciclo. Si el grafo no es
conexo devuelve un **bosque** de expansión mínima (`"conexo": false`).

```bash
curl "http://localhost:8080/api/mst/kruskal"
```

```json
{
  "algoritmo": "Kruskal",
  "origen": null,
  "costoTotalMinutos": 907,
  "aristasEnArbol": 11,
  "nodosCubiertos": 12,
  "conexo": true,
  "aristas": [
    { "desde": "CAMPANA", "hasta": "ZARATE", "minutos": 20 },
    { "desde": "RESISTENCIA", "hasta": "CORRIENTES", "minutos": 22 },
    { "desde": "LOMAS_DE_ZAMORA", "hasta": "QUILMES", "minutos": 25 },
    { "desde": "SANTA_FE", "hasta": "PARANA", "minutos": 28 },
    { "desde": "CABA", "hasta": "LOMAS_DE_ZAMORA", "minutos": 32 },
    { "desde": "PILAR", "hasta": "CAMPANA", "minutos": 35 },
    { "desde": "QUILMES", "hasta": "LA_PLATA", "minutos": 45 },
    { "desde": "CABA", "hasta": "PILAR", "minutos": 55 },
    { "desde": "ROSARIO", "hasta": "SANTA_FE", "minutos": 105 },
    { "desde": "ZARATE", "hasta": "ROSARIO", "minutos": 120 },
    { "desde": "SANTA_FE", "hasta": "RESISTENCIA", "minutos": 420 }
  ]
}
```

**Verificación a mano.** Aristas en orden de peso: 20 ✓, 22 ✓, 25 ✓, 28 ✓, 32 ✓, 35 ✓, **40 ✗** (CABA–QUILMES: ya están
conectados vía Lomas de Zamora), 45 ✓, 55 ✓ (une el GBA con la zona norte), **60 ✗**, **80 ✗**, 105 ✓, 120 ✓,
**150 ✗**, **200 ✗**, 420 ✓, **450 ✗**. Suma de las 11 aristas elegidas:
20 + 22 + 25 + 28 + 32 + 35 + 45 + 55 + 105 + 120 + 420 = **907**. Como los 17 pesos de la semilla son distintos, el
árbol es único y Prim da exactamente las mismas aristas desde cualquier origen.

**Complejidad**

- **Tiempo O(E log V).** Ordenar las E aristas con MergeSort propio: O(E log E) = O(E log V) (porque E ≤ V²). Después
  se recorren una vez, y cada `buscar`/`unir` de Union-Find con compresión de caminos y unión por rango cuesta
  O(α(V)) amortizado (α = inversa de Ackermann, ≤ 4 para cualquier V práctico): O(E α(V)). Domina el ordenamiento.
- **Espacio O(V + E).** Copia del arreglo de aristas (O(E)) + arreglo auxiliar del MergeSort (O(E)) + Union-Find (O(V)).
- **Estructura:** `UnionFind` propio (arreglos `padre` y `rango`). Sin Union-Find, detectar el ciclo con un
  recorrido costaría O(V) por arista y llevaría el total a O(E·V).

---

## 6 y 7. QuickSort y MergeSort sobre los paquetes

### `GET /api/paquetes/ordenar/quicksort?criterio=valor&orden=desc`
### `GET /api/paquetes/ordenar/mergesort?criterio=peso&orden=asc`

| Parámetro | Obligatorio | Valores | Descripción |
|-----------|-------------|---------|-------------|
| `criterio` | no (`valor`) | `valor`, `peso`, `ratio`, `id` | Por qué campo ordenar (`ratio` = valor/peso) |
| `orden` | no (`asc`) | `asc`, `desc` | Sentido |

Ordenan las cajas del grafo. Ante empates desempatan por `id`, así que ambos endpoints devuelven siempre exactamente el
mismo orden para los mismos parámetros. Informan `comparaciones`: cuántas veces se llamó al comparador, lo que permite
ver el comportamiento n log n en vez de solo afirmarlo.

```bash
curl "http://localhost:8080/api/paquetes/ordenar/quicksort?criterio=valor&orden=desc"
```

```json
{
  "algoritmo": "QuickSort",
  "criterio": "valor",
  "orden": "desc",
  "comparaciones": 32,
  "paquetes": [
    { "id": "PQ-04", "descripcion": "Caja Colonias Familiares x24", "pesoKg": 25, "valor": 900, "destinoId": "ROSARIO" },
    { "id": "PQ-03", "descripcion": "Caja Eau de Toilette Caballero x12", "pesoKg": 18, "valor": 760, "destinoId": "PILAR" },
    { "id": "PQ-07", "descripcion": "Caja Perfumes Árabes x6", "pesoKg": 10, "valor": 660, "destinoId": "LOMAS_DE_ZAMORA" },
    "... (abreviado: PQ-05, PQ-02, PQ-01, PQ-06, PQ-08)"
  ]
}
```

Con `mergesort?criterio=peso&orden=asc` el orden es `PQ-08, PQ-01, PQ-07, PQ-02, PQ-05, PQ-03, PQ-06, PQ-04` y
`comparaciones` es 17.

### Complejidad de MergeSort

- **Recurrencia:** T(n) = 2·T(n/2) + Θ(n) (dos mitades y una mezcla lineal). Por el teorema maestro (caso 2,
  a = 2, b = 2, f(n) = Θ(n) = Θ(n^(log₂2))): **T(n) = Θ(n log n)** en el mejor, el promedio y el peor caso.
- **Espacio O(n)** por el arreglo auxiliar único, más O(log n) de pila de recursión.
- **Estable:** al mezclar toma del lado izquierdo salvo que el derecho sea estrictamente menor.

### Complejidad de QuickSort

- **Recurrencia:** T(n) = T(k) + T(n − k − 1) + Θ(n), donde k es el tamaño de la parte menor al pivote.
  Con particiones balanceadas (k ≈ n/2) es T(n) = 2T(n/2) + Θ(n) = **Θ(n log n)**; con particiones totalmente
  desbalanceadas (k = 0 siempre) es T(n) = T(n − 1) + Θ(n) = **Θ(n²)**.
- **Esperado O(n log n), peor caso O(n²).** El pivote es la mediana de tres elementos (primero, medio, último), lo que hace
  muy improbable el peor caso con entradas ya ordenadas o inversas, pero no lo elimina para entradas armadas
  adversarialmente.
- **Partición de tres vías:** agrupa los elementos iguales al pivote y no los vuelve a procesar, así que una entrada con
  muchas claves repetidas (o toda igual) se resuelve en O(n) en vez de degradarse a O(n²).
- **Espacio O(log n):** se recursa solo sobre la mitad más chica y se itera sobre la más grande, por lo que la pila nunca
  supera log₂ n niveles. **No es estable.**

---

## 8. Greedy — selección golosa de paquetes

### `GET /api/paquetes/greedy?capacidadKg=40`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `capacidadKg` | no (`40`) | Capacidad de la camioneta en kg (0 a 100000) |

Ordena las cajas por valor/peso de mayor a menor (con MergeSort propio) y carga cada una si todavía entra. La
respuesta incluye la comparación con el óptimo de la mochila 0/1, para ver cuánto pierde el greedy.

```bash
curl "http://localhost:8080/api/paquetes/greedy?capacidadKg=40"
```

```json
{
  "greedy": {
    "algoritmo": "Greedy por ratio valor/peso",
    "capacidadKg": 40,
    "pesoTotalKg": 36,
    "valorTotal": 1870,
    "paquetesCargados": [ "PQ-07 (10 kg, 660)", "PQ-01 (8 kg, 520)", "PQ-02 (12 kg, 540)", "PQ-08 (6 kg, 150)" ],
    "ordenConsiderado": ["PQ-07", "PQ-01", "PQ-02", "PQ-03", "PQ-05", "PQ-04", "PQ-08", "PQ-06"]
  },
  "comparacionConOptimo": {
    "valorOptimoMochila": 1960,
    "valorPerdidoPorElGreedy": 90,
    "greedyEsOptimo": false
  }
}
```

(`paquetesCargados` abreviado: la respuesta real trae cada caja como objeto completo.)

### `GET /api/paquetes/greedy/contraejemplo`

Juego de datos documentado donde **el greedy NO da el óptimo** (hito 6). Capacidad 50 kg y tres cajas:

| Caja | Peso | Valor | Ratio |
|------|------|-------|-------|
| A | 10 kg | 60 USD | 6,0 |
| B | 20 kg | 100 USD | 5,0 |
| C | 30 kg | 120 USD | 4,0 |

El greedy toma A (mejor ratio) y B: 30 kg, **160 USD**; C ya no entra en los 20 kg que quedan. El óptimo es B + C:
50 kg, **220 USD**. La decisión de quedarse con A es irrevocable y deja un hueco que nada llena bien.

```json
{
  "explicacion": "El greedy toma A y B (30 kg, 160 USD) y C ya no entra en los 20 kg que quedan. El óptimo es B + C (50 kg, 220 USD): el greedy pierde 60 USD.",
  "capacidadKg": 50,
  "juegoDeDatos": [ "CE-A (10 kg, 60)", "CE-B (20 kg, 100)", "CE-C (30 kg, 120)" ],
  "greedy": { "pesoTotalKg": 30, "valorTotal": 160, "...": "..." },
  "optimo": { "valorOptimo": 220, "pesoTotalKg": 50, "...": "..." },
  "valorPerdidoPorElGreedy": 60
}
```

Además, los datos semilla ya exhiben el fallo con la capacidad por defecto: a 40 kg el greedy junta 1870 USD y el
óptimo es 1960 USD.

**Complejidad (ambos endpoints)**

- **Tiempo O(n log n):** ordenar las n cajas con MergeSort (Θ(n log n)) y una pasada lineal para cargarlas (O(n)).
- **Espacio O(n):** el arreglo ordenado y el auxiliar del MergeSort.
- **Por qué no es óptimo:** el criterio "mayor valor por kilo" es una decisión local irrevocable. Para la mochila *fraccionaria*
  el greedy sí es óptimo; para la 0/1 no, porque no se pueden cargar fracciones de caja.

---

## 9. Mochila 0/1 — programación dinámica

### `GET /api/paquetes/mochila?capacidadKg=40&incluirTabla=true`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `capacidadKg` | no (`40`) | Capacidad de la camioneta en kg (0 a 100000) |
| `incluirTabla` | no (`false`) | Si es `true`, incluye la tabla `dp` completa |

Qué cajas cargar en la camioneta para maximizar el valor sin pasar la capacidad. `dp[i][w]` es el mejor valor usando
solo las primeras `i` cajas con capacidad `w`:

```
dp[i][w] = dp[i-1][w]                                      si la caja i no entra (peso_i > w)
dp[i][w] = max( dp[i-1][w],  dp[i-1][w - peso_i] + valor_i )  si entra
```

Las cajas elegidas se **recuperan** recorriendo la tabla desde `dp[n][W]` hacia atrás: si `dp[i][w] != dp[i-1][w]`, la
caja `i` fue tomada y se continúa con `w - peso_i`.

```bash
curl "http://localhost:8080/api/paquetes/mochila?capacidadKg=40"
```

```json
{
  "algoritmo": "Mochila 0/1 (programación dinámica)",
  "capacidadKg": 40,
  "valorOptimo": 1960,
  "pesoTotalKg": 40,
  "paquetesCargados": [
    { "id": "PQ-02", "descripcion": "Caja Eau de Parfum Dama x6", "pesoKg": 12, "valor": 540, "destinoId": "LA_PLATA" },
    { "id": "PQ-03", "descripcion": "Caja Eau de Toilette Caballero x12", "pesoKg": 18, "valor": 760, "destinoId": "PILAR" },
    { "id": "PQ-07", "descripcion": "Caja Perfumes Árabes x6", "pesoKg": 10, "valor": 660, "destinoId": "LOMAS_DE_ZAMORA" }
  ],
  "tablaDp": null
}
```

Con `incluirTabla=true`, `tablaDp` es una matriz de 9 × 41 (n + 1 filas, W + 1 columnas). Algunos valores
(abreviado): la fila 0 es todo ceros; la fila 3 (primeras 3 cajas) vale `[0, 520, 540, 1060, 1300, 1820]` en las
columnas `w = 0, 8, 16, 24, 32, 40`; y `dp[8][40] = 1960`.

**Complejidad**

- **Tiempo Θ(n·W).** Se llena cada una de las (n + 1)·(W + 1) celdas en O(1). La recuperación es O(n).
- **Espacio Θ(n·W)** por la tabla completa (necesaria para recuperar los ítems). Si solo interesara el valor óptimo
  bastaría una fila: O(W). El servicio rechaza tablas de más de 5.000.000 de celdas.
- **Pseudopolinomial:** el costo depende del *valor* de W, no de su cantidad de bits; por eso no contradice que la
  mochila 0/1 sea NP-difícil.

---

## 10. Floyd-Warshall — todos contra todos

### `GET /api/caminos/floyd-warshall`

Sin parámetros. Calcula el tiempo mínimo entre **todos** los pares de nodos y detecta ciclos negativos. Devuelve:
`matrizCostos` (minutos; `null` = sin camino), `matrizSiguiente` (primer salto del camino de i a j: es la **matriz
de caminos** con la que se reconstruye cualquiera) y, para el hito 7, una `comparacionConDijkstraVVeces`.

```bash
curl "http://localhost:8080/api/caminos/floyd-warshall"
```

```json
{
  "floydWarshall": {
    "nodos": ["CABA", "CAMPANA", "CORRIENTES", "LA_PLATA", "LOMAS_DE_ZAMORA", "PARANA",
              "PILAR", "QUILMES", "RESISTENCIA", "ROSARIO", "SANTA_FE", "ZARATE"],
    "tieneCicloNegativo": false,
    "nodosEnCicloNegativo": [],
    "matrizCostos": [
      [0, 80, 747, 85, 32, 333, 55, 40, 725, 200, 305, 100],
      "... (abreviado: 11 filas más; la matriz es simétrica porque el grafo es no dirigido)"
    ],
    "matrizSiguiente": [
      ["CABA", "CAMPANA", "ROSARIO", "QUILMES", "LOMAS_DE_ZAMORA", "ROSARIO",
       "PILAR", "QUILMES", "ROSARIO", "ROSARIO", "ROSARIO", "CAMPANA"],
      "... (abreviado)"
    ],
    "iteracionesInternas": 1728,
    "actualizaciones": 180
  },
  "comparacionConDijkstraVVeces": {
    "dijkstraEjecuciones": 12,
    "iteracionesFloydWarshall": 1728,
    "actualizacionesFloydWarshall": 180,
    "dijkstraNodosExpandidosTotal": 144,
    "dijkstraAristasExaminadasTotal": 408,
    "resultadosCoinciden": true
  }
}
```

**Lectura de la comparación (hito 7).** Floyd-Warshall hace exactamente V³ = 1728 iteraciones del bucle interno.
Correr Dijkstra desde cada uno de los 12 nodos expande 144 nodos (12 × 12) y examina 408 arcos (12 × 34). Las unidades no
son idénticas (iteraciones de un bucle contra expansiones y relajaciones), pero muestran el orden de magnitud: en un grafo
**ralo** como este, V corridas de Dijkstra — O(V·(V + E) log V) — son mucho más baratas que Θ(V³); en un grafo **denso**
(E ≈ V²) Dijkstra V veces sería O(V³ log V) y Floyd-Warshall ganaría. Lo que sí es idéntico es el resultado:
`resultadosCoinciden` verifica las 144 distancias.

### `GET /api/caminos/floyd-warshall/camino?origen=CABA&destino=CORRIENTES`

Reconstruye un camino a partir de la matriz de siguientes, sin volver a calcular nada.

```json
{
  "origen": "CABA",
  "destino": "CORRIENTES",
  "alcanzable": true,
  "costoMinutos": 747,
  "camino": ["CABA", "ROSARIO", "SANTA_FE", "RESISTENCIA", "CORRIENTES"]
}
```

### `GET /api/caminos/floyd-warshall/demo-ciclo-negativo`

El grafo de la red no tiene pesos negativos, así que este endpoint corre Floyd-Warshall sobre un grafo **dirigido**
armado en memoria: A→B (2), B→C (−5), C→A (1) y D→A (4). El ciclo A→B→C→A suma 2 − 5 + 1 = **−2**: se podría dar vueltas
indefinidamente bajando el costo, por lo que los caminos mínimos no están definidos.

```json
{
  "nodos": ["A", "B", "C", "D"],
  "tieneCicloNegativo": true,
  "nodosEnCicloNegativo": ["A", "B", "C"],
  "matrizCostos": null,
  "matrizSiguiente": null,
  "iteracionesInternas": 64,
  "actualizaciones": 17
}
```

D llega al ciclo pero no forma parte de él, por eso no figura en `nodosEnCicloNegativo`. La detección es: al terminar,
algún `d[i][i] < 0`.

**Complejidad**

- **Tiempo Θ(V³):** tres bucles anidados de V iteraciones, cada una con trabajo O(1) (en el mejor y en el peor caso).
  La recurrencia es de programación dinámica: d_k[i][j] = min(d_{k−1}[i][j], d_{k−1}[i][k] + d_{k−1}[k][j]).
- **Espacio Θ(V²):** matriz de costos (`long[V][V]`) y matriz de siguientes (`int[V][V]`), actualizadas en el lugar.
  Reconstruir un camino cuesta O(V).
- **Estructura:** dos matrices. Para evitar desbordes con ciclos negativos, el costo se limita inferiormente a −INF.

---

## 11. Backtracking — rutas simples con restricciones

### `GET /api/backtracking/rutas?origen=CABA&destino=QUILMES&maxMinutos=100&maxSaltos=3`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `origen`, `destino` | sí | Nodos (deben ser distintos) |
| `maxMinutos` | no (`800`) | Tope de tiempo total de la ruta, en minutos |
| `maxSaltos` | no (`6`) | Tope de aristas recorridas (saltos = transbordos + 1), mínimo 1 |
| `limite` | no (`20`) | Cuántas rutas (las más rápidas) incluir en la respuesta (1 a 1000) |

Encuentra **todas** las rutas simples (sin repetir nodos) que cumplen los dos topes. Se ejecuta dos veces sobre el
mismo problema para poder comparar: **con poda** (en cuanto el costo o los saltos acumulados superan un tope, esa rama se
descarta sin explorarla) y **sin poda** (explora todo el árbol de rutas simples y chequea los topes recién al llegar al
destino). Ambas variantes devuelven exactamente las mismas rutas; cambia cuántos nodos del árbol de búsqueda se visitan.

```bash
curl "http://localhost:8080/api/backtracking/rutas?origen=CABA&destino=QUILMES&maxMinutos=100&maxSaltos=3"
```

```json
{
  "origen": "CABA",
  "destino": "QUILMES",
  "maxMinutos": 100,
  "maxSaltos": 3,
  "totalRutasValidas": 2,
  "mejorRuta": { "camino": ["CABA", "QUILMES"], "minutos": 40, "saltos": 1 },
  "rutas": [
    { "camino": ["CABA", "QUILMES"], "minutos": 40, "saltos": 1 },
    { "camino": ["CABA", "LOMAS_DE_ZAMORA", "QUILMES"], "minutos": 57, "saltos": 2 }
  ],
  "nodosExploradosConPoda": 9,
  "nodosExploradosSinPoda": 60,
  "nodosEvitadosPorPoda": 51,
  "reduccionPorcentual": 85.0
}
```

Otro caso, hacia el NEA (`origen=CABA&destino=CORRIENTES&maxMinutos=800&maxSaltos=6`): 4 rutas válidas
(747, 767, 783 y 800 minutos), **43** nodos explorados con poda contra **53** sin poda (18,87 % menos). Con topes tan
holgados que nada se poda (`maxMinutos=5000&maxSaltos=11`) ambos conteos coinciden en 53 y aparecen las 12 rutas
simples que existen entre esos dos nodos.

**Complejidad**

- **Tiempo O(V!) en el peor caso**, con o sin poda: el árbol de rutas simples en un grafo completo tiene Σ V!/(V−j)!
  nodos. La poda reduce cuántos nodos se visitan en la práctica (60 → 9 arriba) pero no cambia el orden en el peor caso.
  Por seguridad el servicio corta con HTTP 422 si una ejecución supera 20.000.000 de nodos explorados.
- **Espacio O(V):** pila de recursión (como mucho V niveles), el arreglo de nodos de la ruta actual y el de "en ruta".
  La lista de rutas encontradas es salida, no espacio auxiliar del algoritmo.
- **Por qué la poda es válida:** los pesos no son negativos, así que el costo acumulado de una ruta nunca baja al
  extenderla; si ya supera el tope, ninguna extensión lo cumplirá.
- **Estructura:** recursión con un arreglo booleano `enRuta` (deshacer la marca al volver = *backtrack*).

---

## 12. Branch & Bound — optimización del reparto

### `GET /api/reparto/branch-and-bound?origen=CABA&destinos=QUILMES,LA_PLATA,PILAR`

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `origen` | no (`CABA`) | Depósito de salida y de regreso |
| `destinos` | no | Nodos donde entregar, separados por coma. Si se omite se usan los destinos de las cajas cargadas |

La camioneta sale del origen, entrega en **todos** los destinos en el orden que convenga y vuelve al origen,
minimizando los minutos totales (un viajante de comercio sobre los destinos). El costo entre dos puntos es el camino
mínimo de la red (Floyd-Warshall en memoria antes de ramificar). Máximo 11 destinos.

**Cómo funciona**

- **Ramificar:** cada nodo del árbol es una ruta parcial `(origen, d1, …, dk)`; sus hijos agregan un destino pendiente,
  probando primero el más cercano para hallar buenas soluciones temprano.
- **Solución inicial:** el recorrido del vecino más cercano da una primera cota superior (`costoSolucionInicial`).
- **Cota explícita y optimista.** Para una ruta parcial que terminó en `v` con costo `c` y destinos pendientes `R`:

  `cota = c + Σ_{u ∈ {v} ∪ R} min{ costo(u, w) : w ∈ R ∪ {origen}, w ≠ u }`

  **Por qué es optimista (nunca sobreestima):** cualquier forma de completar la ruta hace salir exactamente una vez a
  cada nodo de `{v} ∪ R` hacia algún nodo de `R ∪ {origen}`, y cada una de esas salidas cuesta, como mínimo, la más barata
  disponible para ese nodo. La suma de mínimos es entonces una cota inferior del costo restante real.
- **Poda:** si `cota ≥ mejor solución conocida`, ninguna completación puede mejorarla y esa rama se descarta
  (**nodo podado**).

```bash
curl "http://localhost:8080/api/reparto/branch-and-bound?origen=CABA&destinos=QUILMES,LA_PLATA,PILAR"
```

```json
{
  "origen": "CABA",
  "destinos": ["QUILMES", "LA_PLATA", "PILAR"],
  "ordenDeEntrega": ["CABA", "QUILMES", "LA_PLATA", "PILAR", "CABA"],
  "costoTotalMinutos": 280,
  "rutaFisicaCompleta": ["CABA", "QUILMES", "LA_PLATA", "QUILMES", "CABA", "PILAR", "CABA"],
  "cotaActivada": true,
  "cota": "cota = costo acumulado + suma, para cada nodo pendiente (y el actual), de su salida más barata hacia un nodo pendiente o hacia el origen. Es optimista (cota inferior) porque cada nodo debe salir exactamente una vez y nunca por menos que su salida más barata.",
  "costoSolucionInicial": 280,
  "nodosExplorados": 8,
  "nodosPodados": 6,
  "nodosArbolCompleto": 16,
  "porcentajeDelArbolExplorado": 50.0
}
```

Sin `destinos`, usa las 8 localidades de destino de las cajas semilla (resumen):

```json
{
  "origen": "CABA",
  "destinos": ["QUILMES", "LA_PLATA", "PILAR", "ROSARIO", "CAMPANA", "SANTA_FE", "LOMAS_DE_ZAMORA", "CORRIENTES"],
  "ordenDeEntrega": ["CABA", "LOMAS_DE_ZAMORA", "LA_PLATA", "QUILMES", "PILAR", "CAMPANA",
                     "ROSARIO", "SANTA_FE", "CORRIENTES", "CABA"],
  "costoTotalMinutos": 1701,
  "costoSolucionInicial": 1711,
  "nodosExplorados": 4256,
  "nodosPodados": 7785,
  "nodosArbolCompleto": 109601,
  "porcentajeDelArbolExplorado": 3.8832
}
```

Con 8 destinos, la fuerza bruta recorrería 109.601 nodos; la cota deja el árbol en 4.256 (3,88 %) y podó 7.785 ramas, y
llega al mismo óptimo (1701 min, verificado contra una enumeración de las 8! = 40.320 permutaciones). Con los 11 destinos
posibles la búsqueda explora ~520 mil nodos en lugar de ~108 millones (≈ 0,3 s). Los tests verifican en 60 grafos
aleatorios que activar la cota **nunca** cambia el costo óptimo respecto de la fuerza bruta.

**Complejidad**

- **Tiempo, peor caso O(k! · k²)** con k destinos: si la cota no poda nada se recorre todo el árbol, que tiene
  Σ_{j=0..k} k!/(k−j)! ≈ e·k! nodos (`nodosArbolCompleto`), y cada nodo calcula una cota en O(k²). El costo de la
  preparación (Floyd-Warshall) es Θ(V³) y se paga una sola vez. En la práctica la poda reduce el árbol en uno o dos
  órdenes de magnitud, como se ve arriba.
- **Espacio O(k²):** matriz de costos entre destinos; **más O(k)** de pila y de ruta actual. (Floyd-Warshall usa O(V²) al
  preparar.)
- **Estructura:** recursión (búsqueda en profundidad con máscara de bits de visitados) y matriz de costos de camino mínimo.
  Si el árbol supera 50.000.000 de nodos, responde 422.

---

## Errores

Todos los errores tienen el mismo formato, sin stack trace ni nombres de clase, y están a cargo de un
`@RestControllerAdvice` (`ManejadorErrores`). Los detalles de los errores inesperados se escriben solo en el log del
servidor.

| HTTP | Cuándo | Ejemplo de `mensaje` |
|------|--------|----------------------|
| 400 | Parámetro faltante, mal formado o inválido | `Falta el parámetro obligatorio 'destino'` |
| 404 | Nodo inexistente o endpoint inexistente | `El nodo 'ATLANTIDA' no existe. Nodos disponibles: [CABA, ...]` |
| 405 | Método HTTP no permitido | `Método HTTP no permitido para este endpoint` |
| 422 | Una búsqueda exhaustiva superó su tope de nodos | `El backtracking superó el límite de 20000000 nodos explorados...` |
| 503 | Neo4j no disponible | `La base de datos no está disponible en este momento. Intentá de nuevo más tarde.` |
| 500 | Error inesperado (mensaje genérico) | `Ocurrió un error interno. Intentá de nuevo más tarde.` |

```bash
curl -i "http://localhost:8080/api/recorridos/bfs?origen=ATLANTIDA"
```

```json
{
  "timestamp": "2026-10-07T14:40:01.181049876Z",
  "estado": 404,
  "error": "Not Found",
  "mensaje": "El nodo 'ATLANTIDA' no existe. Nodos disponibles: [CABA, CAMPANA, CORRIENTES, LA_PLATA, LOMAS_DE_ZAMORA, PARANA, PILAR, QUILMES, RESISTENCIA, ROSARIO, SANTA_FE, ZARATE]",
  "ruta": "/api/recorridos/bfs"
}
```

---

## Resumen de complejidades (hito 10)

| Algoritmo | Recurrencia | Tiempo | Espacio extra | Estructura de datos que la sostiene |
|-----------|-------------|--------|---------------|-------------------------------------|
| BFS | — | O(V + E) | O(V) | Cola FIFO sobre arreglo |
| DFS | — | O(V + E) | O(V) | Pila de recursión |
| Dijkstra | — | O((V + E) log V) | O(V + E) | Min-heap binario (`MinHeap`) |
| Prim | — | O(E log V) | O(V + E) | Min-heap binario (`MinHeap`) |
| Kruskal | — | O(E log V) | O(V + E) | `UnionFind` (compresión + rango) + MergeSort |
| MergeSort | T(n) = 2T(n/2) + Θ(n) | Θ(n log n) | O(n) | Arreglo auxiliar único |
| QuickSort | T(n) = T(k) + T(n−k−1) + Θ(n) | O(n log n) esperado, O(n²) peor | O(log n) | Partición de tres vías, recursión sobre la mitad menor |
| Greedy | — | O(n log n) | O(n) | Arreglo ordenado por ratio (MergeSort) |
| Mochila 0/1 | dp[i][w] = max(dp[i−1][w], dp[i−1][w−pᵢ] + vᵢ) | Θ(n·W) | Θ(n·W) | Tabla dp `long[n+1][W+1]` |
| Floyd-Warshall | d_k[i][j] = min(d_{k−1}[i][j], d_{k−1}[i][k] + d_{k−1}[k][j]) | Θ(V³) | Θ(V²) | Matrices de costos y de siguientes |
| Backtracking | — (árbol de rutas simples) | O(V!) peor caso | O(V) | Recursión + arreglo `enRuta` |
| Branch & Bound | — (árbol de permutaciones parciales) | O(k!·k²) peor caso | O(k²) | Recursión + máscara de bits + matriz de costos |

## Reglas de la consigna y cómo se cumplen

| Regla | Cómo se cumple | Dónde se verifica |
|-------|----------------|-------------------|
| Sin lógica algorítmica en controllers | Los controllers solo llaman a un servicio; no tienen bucles ni condicionales | `ArquitecturaTest` |
| Sin consultas a la base dentro de un algoritmo | `algorithm` no depende de Spring, Neo4j ni de `repository`; recibe un `GrafoMemoria` | `ArquitecturaTest` |
| Sin stack traces en las respuestas | `ManejadorErrores` + `server.error.include-stacktrace=never` | `ManejadorErroresTest` |
| Sin librerías ni `Collections.sort` | MergeSort/QuickSort, `MinHeap` y `UnionFind` propios; sin `PriorityQueue`, `Arrays.sort` ni `.sorted()` | `ArquitecturaTest` |
| Sin credenciales en el repositorio | Usuario y contraseña salen de variables de entorno sin valor por defecto; `.env` está en `.gitignore` | `ArquitecturaTest` |
