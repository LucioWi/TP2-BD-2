// ═══════════════════════════════════════════════════════════════════════════════
// CYPHER QUERIES — DEBUGGEO Y VISUALIZACIÓN DEL GRAFO DE FRAUDE
// Neo4j Browser  ·  Cytoscape.js ready
// ═══════════════════════════════════════════════════════════════════════════════
//
//  CÓMO USAR EN NEO4J BROWSER
//  ──────────────────────────
//  • Copiar una query, pegar en el editor, presionar ▶ para ejecutar
//  • Queries que retornan n, r, m  → se renderizan como GRAFO visual
//  • Queries que retornan columnas → se renderizan como TABLA
//  • Doble-click sobre un nodo     → expande sus relaciones vecinas
//  • Click en el ícono de grafo    → alterna entre vista graph / table / text
//  • Settings → "Initial node display": subir a 300 si el grafo parece truncado
//  • Usa LIMIT para proteger el browser en grafos densos
//
//  ÍNDICE DE SECCIONES
//  ───────────────────
//  § 1  RESUMEN GENERAL
//  § 2  RED COMPLETA
//  § 3  PERSONAS SOSPECHOSAS
//  § 4  DISPOSITIVOS COMPARTIDOS
//  § 5  DETECCIÓN DE CICLOS
//  § 6  SHORTEST PATH
//  § 7  TRANSFERENCIAS SOSPECHOSAS
//  § 8  COMUNIDADES Y CLUSTERING
//  § 9  ANÁLISIS DE RED (hubs, centralidad)
//  § 10 GDS — Graph Data Science (requiere plugin)
//
// ═══════════════════════════════════════════════════════════════════════════════


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 1  RESUMEN GENERAL                                                       │
// └─────────────────────────────────────────────────────────────────────────────┘

// 1.1  Conteo de nodos por etiqueta ──────────────────────────────────────────
MATCH (n)
RETURN labels(n)[0]  AS tipo,
       COUNT(n)       AS cantidad
ORDER BY cantidad DESC;

// 1.2  Conteo de relaciones por tipo ─────────────────────────────────────────
MATCH ()-[r]->()
RETURN type(r)   AS relacion,
       COUNT(r)  AS cantidad
ORDER BY cantidad DESC;

// 1.3  Resumen de fraude — alertas y riesgos ─────────────────────────────────
MATCH (t:Transaccion)
RETURN
  COUNT(t)                                          AS totalTransacciones,
  SUM(CASE WHEN t.esAlertada  = true  THEN 1 ELSE 0 END) AS alertadas,
  SUM(CASE WHEN t.nivelRiesgo >= 80   THEN 1 ELSE 0 END) AS riesgoAlto,
  SUM(CASE WHEN t.estado = 'BLOQUEADA' OR
               t.estado = 'RECHAZADA'  THEN 1 ELSE 0 END) AS bloqueadasRechazadas,
  AVG(toFloat(t.nivelRiesgo))                       AS riesgoPromedio;

// 1.4  Dashboard personas — distribución de riesgo ───────────────────────────
MATCH (p:Persona)
RETURN
  COUNT(p)                                                   AS total,
  SUM(CASE WHEN p.nivelRiesgo >= 80   THEN 1 ELSE 0 END)    AS criticas,
  SUM(CASE WHEN p.nivelRiesgo >= 50   AND
               p.nivelRiesgo < 80     THEN 1 ELSE 0 END)    AS altas,
  SUM(CASE WHEN p.nivelRiesgo < 50    THEN 1 ELSE 0 END)    AS bajas,
  SUM(CASE WHEN p.esPEP       = true  THEN 1 ELSE 0 END)    AS peps,
  SUM(CASE WHEN p.esSancionado= true  THEN 1 ELSE 0 END)    AS sancionados,
  SUM(CASE WHEN p.verificada  = false THEN 1 ELSE 0 END)    AS sinVerificar;

// 1.5  Propiedades de un nodo específico (debug rápido) ──────────────────────
MATCH (n {id: 'p01'}) RETURN n;
MATCH (n {id: 'c10'}) RETURN n;
MATCH (n {id: 't009'}) RETURN n;
MATCH (n {id: 'd01'}) RETURN n;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 2  RED COMPLETA — VISUALIZACIÓN GLOBAL                                   │
// └─────────────────────────────────────────────────────────────────────────────┘

// 2.1  Grafo completo — todos los nodos y aristas ────────────────────────────
//      ⚠ LIMIT 300 protege el browser; subir si el seed es más grande
MATCH (n)-[r]->(m)
RETURN n, r, m
LIMIT 300;

// 2.2  Solo Personas y Cuentas (sin transacciones ni dispositivos) ────────────
//      Útil para ver la red de titularidad de cuentas
MATCH (p:Persona)-[r:POSEE_CUENTA]->(c:Cuenta)
RETURN p, r, c;

// 2.3  Solo Personas y Dispositivos ──────────────────────────────────────────
MATCH (p:Persona)-[r:USA_DISPOSITIVO]->(d:Dispositivo)
RETURN p, r, d;

// 2.4  Subgrafo de transferencias — Cuentas vinculadas por TRANSFIERE_A ───────
MATCH (co:Cuenta)-[r:TRANSFIERE_A]->(cd:Cuenta)
RETURN co, r, cd;

// 2.5  Subgrafo completo de una persona (ego-network) ────────────────────────
//      Reemplazá 'p01' por cualquier ID de persona
MATCH (p:Persona {id: 'p01'})-[r1]-(nodo1)-[r2]-(nodo2)
RETURN p, r1, nodo1, r2, nodo2
LIMIT 50;

// 2.6  Vecindad de una cuenta (1 y 2 saltos) ─────────────────────────────────
MATCH (c:Cuenta {id: 'c10'})-[r1]-(v1)
OPTIONAL MATCH (v1)-[r2]-(v2)
RETURN c, r1, v1, r2, v2
LIMIT 80;

// 2.7  Red de transacciones alertadas con sus cuentas ────────────────────────
MATCH (t:Transaccion {esAlertada: true})-[r1:ORIGINADA_EN]->(co:Cuenta)
MATCH (t)-[r2:DIRIGIDA_A]->(cd:Cuenta)
RETURN t, r1, co, r2, cd
LIMIT 100;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 3  PERSONAS SOSPECHOSAS                                                  │
// └─────────────────────────────────────────────────────────────────────────────┘

// 3.1  TABULAR — Top personas por nivel de riesgo ────────────────────────────
MATCH (p:Persona)
WHERE p.nivelRiesgo >= 50
OPTIONAL MATCH (p)-[:POSEE_CUENTA]->(c:Cuenta)
WITH p, COUNT(c) AS totalCuentas
RETURN p.id          AS id,
       p.nombre + ' ' + p.apellido AS nombreCompleto,
       p.nivelRiesgo AS riesgo,
       p.esPEP       AS esPEP,
       p.esSancionado AS esSancionado,
       p.verificada  AS verificada,
       totalCuentas
ORDER BY p.nivelRiesgo DESC;

// 3.2  VISUAL — PEP con sus cuentas y transacciones de alto valor ─────────────
MATCH (p:Persona {esPEP: true})-[r1:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (t:Transaccion)-[r2:DIRIGIDA_A]->(c)
WHERE toFloat(t.monto) >= 500000
RETURN p, r1, c, r2, t;

// 3.3  VISUAL — Sancionados con toda su red financiera ───────────────────────
MATCH (p:Persona {esSancionado: true})-[r1]-(vecino)
RETURN p, r1, vecino;

// 3.4  VISUAL — Personas sin verificar con riesgo alto ───────────────────────
MATCH (p:Persona)
WHERE p.verificada = false AND p.nivelRiesgo >= 60
MATCH (p)-[r1:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (p)-[r2:USA_DISPOSITIVO]->(d:Dispositivo)
RETURN p, r1, c, r2, d;

// 3.5  TABULAR — Personas con exceso de cuentas (indicador de mula) ──────────
MATCH (p:Persona)-[:POSEE_CUENTA]->(c:Cuenta)
WITH p, COUNT(c) AS totalCuentas, COLLECT(c.banco) AS bancos
WHERE totalCuentas > 1
RETURN p.nombre + ' ' + p.apellido AS persona,
       p.nivelRiesgo               AS riesgo,
       totalCuentas,
       bancos
ORDER BY totalCuentas DESC;

// 3.6  VISUAL — Red de personas de alto riesgo (>= 70) ──────────────────────
MATCH (p:Persona)
WHERE p.nivelRiesgo >= 70
MATCH (p)-[r1:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (c)-[r2:TRANSFIERE_A]->(c2:Cuenta)<-[:POSEE_CUENTA]-(p2:Persona)
WHERE p2.nivelRiesgo >= 70
RETURN p, r1, c, r2, c2, p2
LIMIT 80;

// 3.7  VISUAL — Personas que comparten dispositivo sospechoso ────────────────
MATCH (p1:Persona)-[:USA_DISPOSITIVO]->(d:Dispositivo {esSospechoso: true})
      <-[:USA_DISPOSITIVO]-(p2:Persona)
WHERE p1 <> p2
MATCH (p1)-[r1:USA_DISPOSITIVO]->(d)
MATCH (p2)-[r2:USA_DISPOSITIVO]->(d)
RETURN p1, r1, d, r2, p2;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 4  DISPOSITIVOS COMPARTIDOS — ACCOUNT TAKEOVER                           │
// └─────────────────────────────────────────────────────────────────────────────┘

// 4.1  TABULAR — Dispositivos con múltiples personas ─────────────────────────
MATCH (d:Dispositivo)<-[:USA_DISPOSITIVO]-(p:Persona)
WITH d, COUNT(DISTINCT p) AS cantPersonas,
        COLLECT(p.nombre + ' ' + p.apellido) AS personas
WHERE cantPersonas > 1
RETURN d.id          AS dispositivoId,
       d.fingerprint AS fingerprint,
       d.ipAddress   AS ip,
       d.ipPais      AS pais,
       d.ipEsTor     AS esTor,
       d.ipEsVPN     AS esVPN,
       d.esEmulador  AS esEmulador,
       cantPersonas,
       personas
ORDER BY cantPersonas DESC;

// 4.2  VISUAL — Red de account takeover completa (dispositivo d01) ────────────
MATCH (p:Persona)-[r1:USA_DISPOSITIVO]->(d:Dispositivo {id: 'd01'})
MATCH (p)-[r2:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (c)-[r3:USADA_EN]->(d)
RETURN p, r1, d, r2, c, r3;

// 4.3  VISUAL — Cuentas de diferentes titulares operadas desde el mismo dispositivo
MATCH (c1:Cuenta)-[:USADA_EN]->(d:Dispositivo)<-[:USADA_EN]-(c2:Cuenta)
MATCH (p1:Persona)-[:POSEE_CUENTA]->(c1)
MATCH (p2:Persona)-[:POSEE_CUENTA]->(c2)
WHERE p1 <> p2 AND c1 <> c2
MATCH (c1)-[r1:USADA_EN]->(d)
MATCH (c2)-[r2:USADA_EN]->(d)
RETURN p1, c1, r1, d, r2, c2, p2;

// 4.4  VISUAL — Todos los dispositivos sospechosos con su red completa ─────────
MATCH (d:Dispositivo)
WHERE d.esSospechoso = true
   OR d.ipEsTor      = true
   OR d.esEmulador   = true
   OR d.ipEsVPN      = true
MATCH (p:Persona)-[r1:USA_DISPOSITIVO]->(d)
OPTIONAL MATCH (p)-[r2:POSEE_CUENTA]->(c:Cuenta)
RETURN d, r1, p, r2, c;

// 4.5  TABULAR — Ranking dispositivos por operaciones y personas asociadas ────
MATCH (d:Dispositivo)<-[:USA_DISPOSITIVO]-(p:Persona)
WITH d, COUNT(DISTINCT p) AS personas
MATCH (d)<-[:USADA_EN]-(c:Cuenta)
WITH d, personas, COUNT(DISTINCT c) AS cuentas
RETURN d.id               AS id,
       d.fingerprint      AS fingerprint,
       d.ipAddress        AS ip,
       d.ipEsTor          AS tor,
       d.ipEsVPN          AS vpn,
       d.esEmulador       AS emulador,
       d.esRooteado       AS rooteado,
       d.esSospechoso     AS sospechoso,
       personas,
       cuentas,
       d.cantidadSesiones AS sesiones
ORDER BY personas DESC, cuentas DESC;

// 4.6  VISUAL — Identidad sintética: d02 con sus 3 cuentas y transacciones ────
MATCH (d:Dispositivo {id: 'd02'})<-[r1:USA_DISPOSITIVO]-(p:Persona)
MATCH (p)-[r2:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (t:Transaccion)-[r3:ORIGINADA_EN]->(c)
RETURN d, r1, p, r2, c, r3, t;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 5  DETECCIÓN DE CICLOS — LAVADO CIRCULAR Y SMURFING                      │
// └─────────────────────────────────────────────────────────────────────────────┘

// 5.1  VISUAL — Ciclos circulares de 3 saltos (A→B→C→A) ─────────────────────
//      El patrón más claro de layering en lavado de activos
MATCH p = (c:Cuenta)-[:TRANSFIERE_A*3]->(c)
RETURN p
LIMIT 5;

// 5.2  VISUAL — Ciclos de cualquier longitud entre 2 y 6 saltos ───────────────
MATCH p = (c:Cuenta)-[:TRANSFIERE_A*2..6]->(c)
RETURN p
LIMIT 10;

// 5.3  TABULAR — Listar todas las cuentas involucradas en ciclos ──────────────
MATCH ciclo = (c:Cuenta)-[:TRANSFIERE_A*2..6]->(c)
RETURN DISTINCT [n IN nodes(ciclo) | n.id]      AS cuentasEnCiclo,
                [n IN nodes(ciclo) | n.banco]    AS bancos,
                [n IN nodes(ciclo) | n.saldo]    AS saldos,
                length(ciclo)                    AS saltos
ORDER BY saltos;

// 5.4  VISUAL — Red completa del lavado circular (incluye personas titulares) ─
MATCH (c:Cuenta)-[:TRANSFIERE_A*2..6]->(c)
WITH COLLECT(DISTINCT c) AS cuentasCiclo
UNWIND cuentasCiclo AS cc
MATCH (p:Persona)-[r1:POSEE_CUENTA]->(cc)
MATCH (cc)-[r2:TRANSFIERE_A]->(cc2:Cuenta)
WHERE cc2 IN cuentasCiclo
RETURN p, r1, cc, r2, cc2;

// 5.5  VISUAL — Ciclo específico: Carlos → Ana → Roberto → Carlos ─────────────
MATCH p = (c1:Cuenta {id:'c01'})-[:TRANSFIERE_A*..6]->(c1)
RETURN p
LIMIT 3;

// 5.6  TABULAR — Ciclos con monto total acumulado ────────────────────────────
MATCH ciclo = (c:Cuenta)-[:TRANSFIERE_A*2..4]->(c)
WITH ciclo,
     [r IN relationships(ciclo) | toFloat(r.monto)] AS montos,
     [n IN nodes(ciclo) | n.id]                     AS ids
RETURN DISTINCT ids                       AS ciclo,
                REDUCE(s=0.0, m IN montos | s + m) AS montoTotal,
                length(ciclo)             AS saltos
ORDER BY montoTotal DESC;

// 5.7  VISUAL — Mini ciclo de identidad sintética (c11→c12→c13→c11) ──────────
MATCH p = (c:Cuenta {id:'c11'})-[:TRANSFIERE_A*..6]->(c)
RETURN p
LIMIT 3;

// 5.8  VISUAL — Anillo de fraude extendido: ciclo + personas + dispositivos ───
MATCH (c:Cuenta)-[:TRANSFIERE_A*2..6]->(c)
WITH COLLECT(DISTINCT c) AS cuentasCiclo
UNWIND cuentasCiclo AS cc
MATCH (p:Persona)-[r1:POSEE_CUENTA]->(cc)
MATCH (p)-[r2:USA_DISPOSITIVO]->(d:Dispositivo)
RETURN cc, p, r1, r2, d
LIMIT 60;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 6  SHORTEST PATH — CAMINOS ENTRE NODOS                                   │
// └─────────────────────────────────────────────────────────────────────────────┘

// 6.1  VISUAL — Camino más corto entre dos personas (cualquier relación) ───────
//      Pregunta: ¿Están conectados el lavador y la mula?
MATCH (p1:Persona {id: 'p01'}), (p2:Persona {id: 'p12'})
MATCH camino = shortestPath((p1)-[*..10]-(p2))
RETURN camino;

// 6.2  VISUAL — Camino más corto entre el sospechoso y el sancionado ──────────
MATCH (p1:Persona {id: 'p03'}), (p2:Persona {id: 'p14'})
MATCH camino = shortestPath((p1)-[*..12]-(p2))
RETURN camino;

// 6.3  VISUAL — Camino más corto entre dos cuentas (rastreo de fondos) ────────
//      Pregunta: ¿Llegó dinero de c01 a c10 (la mula)?
MATCH (c1:Cuenta {id: 'c01'}), (c2:Cuenta {id: 'c10'})
MATCH camino = shortestPath((c1)-[:TRANSFIERE_A*..10]-(c2))
RETURN camino;

// 6.4  VISUAL — Camino más corto entre dos transacciones sospechosas ──────────
//      Pregunta: ¿Están conectadas la transferencia circular y el smurfing?
MATCH (t1:Transaccion {id: 't001'}), (t2:Transaccion {id: 't009'})
MATCH camino = shortestPath((t1)-[:RELACIONADA_CON*..8]-(t2))
RETURN camino;

// 6.5  TABULAR — Distancia entre cada par de personas sospechosas ─────────────
MATCH (p1:Persona), (p2:Persona)
WHERE p1.nivelRiesgo >= 75 AND p2.nivelRiesgo >= 75 AND p1 <> p2
MATCH camino = shortestPath((p1)-[*..10]-(p2))
RETURN p1.nombre + ' ' + p1.apellido  AS origen,
       p2.nombre + ' ' + p2.apellido  AS destino,
       length(camino)                  AS saltos
ORDER BY saltos ASC
LIMIT 20;

// 6.6  VISUAL — Todos los caminos (no solo el más corto) entre dos cuentas ────
//      ⚠ Puede ser lento si el grafo es denso — usar LIMIT siempre
MATCH (c1:Cuenta {id: 'c01'}), (c2:Cuenta {id: 'c16'})
MATCH caminos = allShortestPaths((c1)-[:TRANSFIERE_A*..8]-(c2))
RETURN caminos
LIMIT 5;

// 6.7  TABULAR — Distancia media del hub a todos los demás nodos ──────────────
//      Victor Huerta (p15/c16) es el hub — mide qué tan central es
MATCH (hub:Cuenta {id: 'c16'}), (otra:Cuenta)
WHERE hub <> otra
MATCH camino = shortestPath((hub)-[:TRANSFIERE_A*..10]-(otra))
RETURN otra.id                     AS cuentaDestino,
       otra.banco                  AS banco,
       length(camino)              AS distancia
ORDER BY distancia ASC;

// 6.8  VISUAL — Camino completo: desde identidad sintética hasta el hub ───────
MATCH (p_sint:Persona {id:'p09'}), (hub:Persona {id:'p15'})
MATCH camino = shortestPath((p_sint)-[*..12]-(hub))
RETURN camino;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 7  TRANSFERENCIAS SOSPECHOSAS                                             │
// └─────────────────────────────────────────────────────────────────────────────┘

// 7.1  VISUAL — Red de RELACIONADA_CON (anillo de fraude completo) ─────────────
MATCH (t1:Transaccion)-[r:RELACIONADA_CON]->(t2:Transaccion)
RETURN t1, r, t2;

// 7.2  VISUAL — Anillo de fraude desde una transacción específica (1-4 saltos) ─
MATCH (t:Transaccion {id: 't001'})-[r:RELACIONADA_CON*1..4]-(tRel:Transaccion)
WHERE tRel <> t
WITH COLLECT(DISTINCT tRel) AS anillo, t
UNWIND anillo AS tr
MATCH camino = shortestPath((t)-[:RELACIONADA_CON*..4]-(tr))
RETURN camino
LIMIT 30;

// 7.3  TABULAR — Smurfing: muchas transferencias pequeñas al mismo destino ────
MATCH (t:Transaccion)-[:DIRIGIDA_A]->(destino:Cuenta)
WHERE toFloat(t.monto) < 10000 AND t.esAlertada = true
WITH destino,
     COUNT(t)               AS totalRecibidas,
     SUM(toFloat(t.monto))  AS montoAgregado,
     MIN(t.fechaCreacion)   AS primera,
     MAX(t.fechaCreacion)   AS ultima
WHERE totalRecibidas >= 3
RETURN destino.id              AS cuentaDestino,
       destino.banco           AS banco,
       totalRecibidas,
       montoAgregado,
       primera,
       ultima
ORDER BY totalRecibidas DESC;

// 7.4  VISUAL — Red de smurfing: fuentes, mula y transacciones ────────────────
MATCH (t:Transaccion)-[r1:DIRIGIDA_A]->(mula:Cuenta {id:'c10'})
WHERE t.esAlertada = true
MATCH (t)-[r2:ORIGINADA_EN]->(origen:Cuenta)
OPTIONAL MATCH (mula)-[r3:TRANSFIERE_A]->(salida:Cuenta)
RETURN t, r1, mula, r2, origen, r3, salida;

// 7.5  TABULAR — Velocity check: cuentas con muchas transacciones en 24 hs ────
MATCH (t:Transaccion)-[:ORIGINADA_EN]->(c:Cuenta)
WHERE t.fechaCreacion >= localdatetime('2024-11-01T00:00:00')
  AND t.fechaCreacion <= localdatetime('2024-11-01T23:59:59')
WITH c, COUNT(t) AS totalTx, SUM(toFloat(t.monto)) AS montoTotal
WHERE totalTx >= 2
RETURN c.id            AS cuentaId,
       c.banco         AS banco,
       totalTx         AS transaccionesEn24h,
       montoTotal      AS montoAcumulado
ORDER BY totalTx DESC;

// 7.6  VISUAL — Velocity: origen con múltiples salidas en ventana de tiempo ────
MATCH (t:Transaccion)-[r1:ORIGINADA_EN]->(c:Cuenta)
WHERE t.fechaCreacion >= localdatetime('2024-11-01T00:00:00')
  AND t.fechaCreacion <= localdatetime('2024-11-01T23:59:59')
WITH c, COLLECT(t) AS txs
WHERE SIZE(txs) >= 2
UNWIND txs AS t
MATCH (t)-[r2:DIRIGIDA_A]->(destino:Cuenta)
RETURN c, t, r1, r2, destino;

// 7.7  VISUAL — Transacciones bloqueadas y rechazadas con sus cuentas ──────────
MATCH (t:Transaccion)-[r1:ORIGINADA_EN]->(co:Cuenta)
MATCH (t)-[r2:DIRIGIDA_A]->(cd:Cuenta)
WHERE t.estado IN ['BLOQUEADA', 'RECHAZADA']
RETURN t, r1, co, r2, cd;

// 7.8  TABULAR — Top transferencias por monto con indicadores de fraude ────────
MATCH (t:Transaccion)-[:ORIGINADA_EN]->(co:Cuenta)
MATCH (t)-[:DIRIGIDA_A]->(cd:Cuenta)
RETURN t.id           AS id,
       t.numeroOrden  AS orden,
       t.monto        AS monto,
       t.moneda       AS moneda,
       t.estado       AS estado,
       t.nivelRiesgo  AS riesgo,
       t.esAlertada   AS alertada,
       t.motivoAlerta AS motivo,
       co.id          AS origen,
       cd.id          AS destino,
       t.fechaCreacion AS fecha
ORDER BY toFloat(t.monto) DESC
LIMIT 20;

// 7.9  VISUAL — Flujo completo del dinero: persona → cuenta → tx → cuenta → persona
MATCH (po:Persona)-[r1:POSEE_CUENTA]->(co:Cuenta)
      <-[r2:ORIGINADA_EN]-(t:Transaccion)-[r3:DIRIGIDA_A]->(cd:Cuenta)
      <-[r4:POSEE_CUENTA]-(pd:Persona)
WHERE t.esAlertada = true
RETURN po, r1, co, r2, t, r3, cd, r4, pd
LIMIT 60;

// 7.10 TABULAR — Distribución de montos por rango (heatmap de montos) ─────────
MATCH (t:Transaccion)
RETURN
  SUM(CASE WHEN toFloat(t.monto) < 10000                          THEN 1 ELSE 0 END) AS `<10K`,
  SUM(CASE WHEN toFloat(t.monto) >= 10000 AND toFloat(t.monto) < 50000   THEN 1 ELSE 0 END) AS `10K-50K`,
  SUM(CASE WHEN toFloat(t.monto) >= 50000 AND toFloat(t.monto) < 200000  THEN 1 ELSE 0 END) AS `50K-200K`,
  SUM(CASE WHEN toFloat(t.monto) >= 200000 AND toFloat(t.monto) < 500000 THEN 1 ELSE 0 END) AS `200K-500K`,
  SUM(CASE WHEN toFloat(t.monto) >= 500000                        THEN 1 ELSE 0 END) AS `>500K`;

// 7.11 TABULAR — Timeline de transacciones alertadas (ordenadas por fecha) ────
MATCH (t:Transaccion)-[:ORIGINADA_EN]->(co:Cuenta)
MATCH (t)-[:DIRIGIDA_A]->(cd:Cuenta)
WHERE t.esAlertada = true
RETURN t.fechaCreacion        AS fecha,
       t.id                   AS txId,
       t.monto                AS monto,
       t.nivelRiesgo          AS riesgo,
       t.motivoAlerta         AS alerta,
       co.id + '→' + cd.id   AS flujo
ORDER BY t.fechaCreacion ASC;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 8  COMUNIDADES Y CLUSTERING                                               │
// └─────────────────────────────────────────────────────────────────────────────┘

// 8.1  VISUAL — Comunidad del lavado circular (todos los nodos del cluster) ────
MATCH (p:Persona)-[r1:POSEE_CUENTA]->(c:Cuenta)-[r2:TRANSFIERE_A*1..3]->(c2:Cuenta)
      <-[r3:POSEE_CUENTA]-(p2:Persona)
WHERE c2 <> c AND p2 <> p
WITH COLLECT(DISTINCT p) + COLLECT(DISTINCT p2) AS personas,
     COLLECT(DISTINCT c) + COLLECT(DISTINCT c2) AS cuentas
UNWIND personas AS per
UNWIND cuentas AS cue
MATCH (per)-[r:POSEE_CUENTA]->(cue)
RETURN per, r, cue;

// 8.2  VISUAL — Comunidad del smurfing (orígenes + mula + salidas) ─────────────
MATCH (origen:Cuenta)-[:TRANSFIERE_A]->(mula:Cuenta {id:'c10'})
WITH COLLECT(DISTINCT origen) AS origenes, mula
UNWIND origenes AS o
MATCH (p:Persona)-[r1:POSEE_CUENTA]->(o)
MATCH (o)-[r2:TRANSFIERE_A]->(mula)
OPTIONAL MATCH (mula)-[r3:TRANSFIERE_A]->(salida:Cuenta)
RETURN p, r1, o, r2, mula, r3, salida;

// 8.3  VISUAL — Comunidad de identidad sintética (personas + cuentas + device) ─
MATCH (d:Dispositivo {id:'d02'})<-[r1:USA_DISPOSITIVO]-(p:Persona)
MATCH (p)-[r2:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (c)-[r3:TRANSFIERE_A]->(c2:Cuenta)
RETURN d, r1, p, r2, c, r3, c2;

// 8.4  VISUAL — Personas conectadas por transacciones (2 saltos) ──────────────
//      Revela quien está vinculado con quien en la red financiera
MATCH (p1:Persona)-[:POSEE_CUENTA]->(c1:Cuenta)-[r1:TRANSFIERE_A]->(c2:Cuenta)
      <-[:POSEE_CUENTA]-(p2:Persona)
WHERE p1 <> p2
RETURN p1, c1, r1, c2, p2
LIMIT 80;

// 8.5  TABULAR — Contactos financieros por persona (grado de salida) ──────────
//      Una persona conectada a muchos titulares distintos = nodo central de fraude
MATCH (p:Persona)-[:POSEE_CUENTA]->(c1:Cuenta)-[:TRANSFIERE_A]->(c2:Cuenta)
      <-[:POSEE_CUENTA]-(p2:Persona)
WHERE p <> p2
WITH p, COUNT(DISTINCT p2) AS contactosFinancieros
RETURN p.id                                AS id,
       p.nombre + ' ' + p.apellido         AS persona,
       p.nivelRiesgo                       AS riesgo,
       contactosFinancieros
ORDER BY contactosFinancieros DESC
LIMIT 15;

// 8.6  VISUAL — Componente conectada completa a partir del hub ────────────────
//      Expande desde Victor Huerta (p15) hasta profundidad 4
MATCH (hub:Persona {id:'p15'})-[r*1..4]-(nodo)
WITH COLLECT(DISTINCT nodo) AS nodos, COLLECT(r) AS rels
UNWIND rels AS rel
RETURN startNode(rel), rel, endNode(rel)
LIMIT 150;

// 8.7  TABULAR — Componentes bi-conectadas: cuentas con alto tráfico bilateral ─
//      Cuentas que envían Y reciben = candidatas a cuenta puente (money mule)
MATCH (c:Cuenta)
WITH c,
     SIZE([(t:Transaccion)-[:DIRIGIDA_A]->(c)  | t]) AS recibidas,
     SIZE([(t:Transaccion)-[:ORIGINADA_EN]->(c) | t]) AS enviadas
WHERE recibidas > 0 AND enviadas > 0
RETURN c.id            AS cuentaId,
       c.banco         AS banco,
       c.tipoCuenta    AS tipo,
       c.estado        AS estado,
       enviadas,
       recibidas,
       (enviadas + recibidas) AS totalMovimientos
ORDER BY totalMovimientos DESC;

// 8.8  VISUAL — Cuenta puente + toda su red de entrada y salida ───────────────
MATCH (t1:Transaccion)-[r1:DIRIGIDA_A]->(puente:Cuenta {id:'c10'})
MATCH (t2:Transaccion)-[r2:ORIGINADA_EN]->(puente)
MATCH (t1)-[r3:ORIGINADA_EN]->(entrada:Cuenta)
MATCH (t2)-[r4:DIRIGIDA_A]->(salida:Cuenta)
RETURN t1, r1, puente, r2, t2, r3, entrada, r4, salida;

// 8.9  TABULAR — Densidad de la red por banco ─────────────────────────────────
MATCH (co:Cuenta)-[:TRANSFIERE_A]->(cd:Cuenta)
RETURN co.banco AS bancoOrigen,
       cd.banco AS bancoDestino,
       COUNT(*) AS cantTransferencias
ORDER BY cantTransferencias DESC;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 9  ANÁLISIS DE RED — HUBS Y CENTRALIDAD (sin GDS)                        │
// └─────────────────────────────────────────────────────────────────────────────┘

// 9.1  TABULAR — Grado de entrada y salida de cada cuenta (in/out degree) ──────
MATCH (c:Cuenta)
OPTIONAL MATCH (c)-[:TRANSFIERE_A]->(out:Cuenta)
OPTIONAL MATCH (c)<-[:TRANSFIERE_A]-(in:Cuenta)
WITH c,
     COUNT(DISTINCT out) AS gradoSalida,
     COUNT(DISTINCT in)  AS gradoEntrada
RETURN c.id                        AS id,
       c.banco                     AS banco,
       c.tipoCuenta                AS tipo,
       gradoEntrada,
       gradoSalida,
       (gradoEntrada + gradoSalida) AS gradoTotal
ORDER BY gradoTotal DESC
LIMIT 15;

// 9.2  TABULAR — Betweenness aproximado: nodos que aparecen en más caminos ─────
//      Cuenta cuántos pares de cuentas "pasan por" cada nodo como intermediario
MATCH (c1:Cuenta)-[:TRANSFIERE_A*2..5]->(c2:Cuenta)
WHERE c1 <> c2
MATCH p = shortestPath((c1)-[:TRANSFIERE_A*..5]->(c2))
UNWIND nodes(p)[1..-1] AS intermediario
RETURN intermediario.id                  AS cuentaId,
       intermediario.banco               AS banco,
       COUNT(*)                          AS vecesIntermediaria
ORDER BY vecesIntermediaria DESC
LIMIT 10;

// 9.3  TABULAR — Volumen total enviado y recibido por cuenta ──────────────────
MATCH (c:Cuenta)
OPTIONAL MATCH (tx_out:Transaccion)-[:ORIGINADA_EN]->(c)
OPTIONAL MATCH (tx_in:Transaccion)-[:DIRIGIDA_A]->(c)
WITH c,
     SUM(toFloat(tx_out.monto)) AS volumenEnviado,
     SUM(toFloat(tx_in.monto))  AS volumenRecibido,
     COUNT(DISTINCT tx_out)     AS txEnviadas,
     COUNT(DISTINCT tx_in)      AS txRecibidas
RETURN c.id             AS id,
       c.banco          AS banco,
       txEnviadas,
       txRecibidas,
       ROUND(COALESCE(volumenEnviado,  0.0), 2) AS volumenEnviado,
       ROUND(COALESCE(volumenRecibido, 0.0), 2) AS volumenRecibido
ORDER BY (COALESCE(volumenEnviado,0) + COALESCE(volumenRecibido,0)) DESC
LIMIT 15;

// 9.4  VISUAL — Subgrafo de las 5 cuentas más conectadas ─────────────────────
MATCH (c:Cuenta)
OPTIONAL MATCH (c)-[:TRANSFIERE_A]-(vecino:Cuenta)
WITH c, COUNT(DISTINCT vecino) AS grado
ORDER BY grado DESC
LIMIT 5
MATCH (c)-[r:TRANSFIERE_A]-(v:Cuenta)
RETURN c, r, v;

// 9.5  TABULAR — IP más frecuentes en transacciones alertadas ────────────────
MATCH (t:Transaccion)
WHERE t.esAlertada = true AND t.ipAddress IS NOT NULL
RETURN t.ipAddress                  AS ip,
       COUNT(t)                     AS transacciones,
       AVG(toFloat(t.nivelRiesgo))  AS riesgoPromedio
ORDER BY transacciones DESC;

// 9.6  TABULAR — Canal de transacción más usado en fraudes ───────────────────
MATCH (t:Transaccion {esAlertada: true})
RETURN t.canal                      AS canal,
       COUNT(t)                     AS totalAlertas,
       AVG(toFloat(t.nivelRiesgo))  AS riesgoPromedio,
       SUM(toFloat(t.monto))        AS montoTotal
ORDER BY totalAlertas DESC;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § 10 GRAPH DATA SCIENCE — requiere Neo4j GDS plugin                        │
// │       Instalar: https://neo4j.com/docs/graph-data-science/current/          │
// └─────────────────────────────────────────────────────────────────────────────┘

// 10.1  Verificar si GDS está instalado ──────────────────────────────────────
RETURN gds.version() AS gdsVersion;

// 10.2  Crear proyección del grafo de cuentas ────────────────────────────────
CALL gds.graph.project(
  'red-cuentas',
  'Cuenta',
  { TRANSFIERE_A: { orientation: 'UNDIRECTED' } }
);

// 10.3  PageRank — detecta cuentas centrales en la red ───────────────────────
CALL gds.pageRank.stream('red-cuentas')
YIELD nodeId, score
WITH gds.util.asNode(nodeId) AS cuenta, score
RETURN cuenta.id     AS cuentaId,
       cuenta.banco  AS banco,
       ROUND(score, 4) AS pageRank
ORDER BY pageRank DESC
LIMIT 10;

// 10.4  Weakly Connected Components — detecta componentes aisladas ────────────
CALL gds.wcc.stream('red-cuentas')
YIELD nodeId, componentId
WITH componentId, COLLECT(gds.util.asNode(nodeId).id) AS miembros
RETURN componentId,
       SIZE(miembros)  AS tamano,
       miembros
ORDER BY tamano DESC
LIMIT 10;

// 10.5  Louvain Community Detection — detecta comunidades (clusters de fraude) ─
CALL gds.louvain.stream('red-cuentas')
YIELD nodeId, communityId
WITH communityId, COLLECT(gds.util.asNode(nodeId)) AS miembros
RETURN communityId,
       SIZE(miembros)                              AS tamano,
       [m IN miembros | m.id]                     AS cuentas,
       [m IN miembros | m.banco]                  AS bancos
ORDER BY tamano DESC
LIMIT 10;

// 10.6  Node Similarity — pares de cuentas con comportamiento similar ─────────
CALL gds.nodeSimilarity.stream('red-cuentas')
YIELD node1, node2, similarity
WITH gds.util.asNode(node1) AS c1,
     gds.util.asNode(node2) AS c2,
     similarity
WHERE similarity > 0.5
RETURN c1.id AS cuenta1, c2.id AS cuenta2, ROUND(similarity, 3) AS similitud
ORDER BY similitud DESC
LIMIT 20;

// 10.7  Limpiar la proyección cuando ya no se necesite ────────────────────────
CALL gds.graph.drop('red-cuentas') YIELD graphName;


// ┌─────────────────────────────────────────────────────────────────────────────┐
// │  § BONUS — QUERIES COMPUESTAS PARA INVESTIGACIÓN                            │
// └─────────────────────────────────────────────────────────────────────────────┘

// B.1  VISUAL — "Show me everything suspicious" ──────────────────────────────
//      Un solo query para tener el panorama completo de fraude
MATCH (t:Transaccion)-[r1:ORIGINADA_EN]->(co:Cuenta)
MATCH (t)-[r2:DIRIGIDA_A]->(cd:Cuenta)
WHERE t.nivelRiesgo >= 75
OPTIONAL MATCH (p1:Persona)-[r3:POSEE_CUENTA]->(co)
OPTIONAL MATCH (p2:Persona)-[r4:POSEE_CUENTA]->(cd)
OPTIONAL MATCH (co)-[r5:USADA_EN]->(d:Dispositivo {esSospechoso: true})
RETURN t, r1, co, r2, cd, r3, p1, r4, p2, r5, d
LIMIT 120;

// B.2  VISUAL — Cadena completa de un caso de fraude (p01 → hub) ──────────────
//      Muestra cómo el dinero de Carlos Mendez llega a Victor Huerta
MATCH camino = (p_origen:Persona {id:'p01'})-[*..8]-(p_hub:Persona {id:'p15'})
RETURN camino
LIMIT 5;

// B.3  TABULAR — Reporte de KYC: personas que requieren revisión urgente ───────
MATCH (p:Persona)
OPTIONAL MATCH (p)-[:POSEE_CUENTA]->(c:Cuenta)
OPTIONAL MATCH (p)-[:USA_DISPOSITIVO]->(d:Dispositivo)
WITH p,
     COUNT(DISTINCT c) AS cuentas,
     SUM(CASE WHEN d.esSospechoso = true THEN 1 ELSE 0 END) AS dispositivosSosp
RETURN p.id                                AS id,
       p.nombre + ' ' + p.apellido        AS nombre,
       p.tipoDocumento + ':' + p.numeroDocumento AS documento,
       p.nivelRiesgo                      AS riesgo,
       p.esPEP                            AS pep,
       p.esSancionado                     AS sancionado,
       p.verificada                       AS verificada,
       cuentas,
       dispositivosSosp,
       CASE
         WHEN p.esSancionado = true        THEN '🔴 BLOQUEO INMEDIATO'
         WHEN p.nivelRiesgo >= 90          THEN '🔴 REVISIÓN URGENTE'
         WHEN p.esPEP = true               THEN '🟡 EDD REQUERIDO'
         WHEN p.nivelRiesgo >= 70          THEN '🟡 MONITOREO ACTIVO'
         WHEN dispositivosSosp > 0         THEN '🟡 DEVICE ALERT'
         ELSE                                   '🟢 OK'
       END AS clasificacion
ORDER BY p.nivelRiesgo DESC;

// B.4  VISUAL — Grafo de investigación: una transacción y su contexto completo ─
//      Reemplazá 't001' por cualquier ID de transacción a investigar
MATCH (t:Transaccion {id: 't001'})
OPTIONAL MATCH (t)-[r1:ORIGINADA_EN]->(co:Cuenta)
OPTIONAL MATCH (t)-[r2:DIRIGIDA_A]->(cd:Cuenta)
OPTIONAL MATCH (po:Persona)-[r3:POSEE_CUENTA]->(co)
OPTIONAL MATCH (pd:Persona)-[r4:POSEE_CUENTA]->(cd)
OPTIONAL MATCH (co)-[r5:USADA_EN]->(do:Dispositivo)
OPTIONAL MATCH (cd)-[r6:USADA_EN]->(dd:Dispositivo)
OPTIONAL MATCH (t)-[r7:RELACIONADA_CON*1..2]-(tRel:Transaccion)
RETURN t, r1, co, r2, cd, r3, po, r4, pd, r5, do, r6, dd, r7, tRel;

// B.5  TABULAR — Matriz de riesgo: cuenta × dispositivo × persona ─────────────
MATCH (p:Persona)-[:USA_DISPOSITIVO]->(d:Dispositivo)
MATCH (p)-[:POSEE_CUENTA]->(c:Cuenta)
MATCH (c)-[:USADA_EN]->(d)
RETURN p.nombre + ' ' + p.apellido     AS persona,
       p.nivelRiesgo                   AS riesgoPersona,
       c.id                            AS cuenta,
       c.banco                         AS banco,
       d.fingerprint                   AS dispositivo,
       d.ipAddress                     AS ip,
       d.ipEsTor     AS tor,
       d.ipEsVPN     AS vpn,
       d.esEmulador  AS emulador,
       d.esRooteado  AS rooteado,
       d.esSospechoso AS sospechoso
ORDER BY riesgoPersona DESC;
