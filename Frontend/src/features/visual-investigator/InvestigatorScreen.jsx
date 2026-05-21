import { useState, useEffect, useCallback } from 'react';
import { Network } from 'lucide-react';
import GraphCanvas from './GraphCanvas';
import DidacticPanel from './DidacticPanel';
import GraphLegend from '../../components/GraphLegend';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Spinner from '../../components/ui/Spinner';
import Badge from '../../components/ui/Badge';
import Switch from '../../components/ui/Switch';
import apiClient from '../../services/apiClient';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import {
  mapShortestPathToCytoscape,
  mapCicloToCytoscape,
  mapIdentidadSinteticaToCytoscape,
} from '../../utils/mappingCytoscape';

const PATRONES = [
  { id: 'lavado-circular', label: 'Lavado Circular' },
  { id: 'identidad-sintetica', label: 'Identidad Sintética' },
  { id: 'shortest-path', label: 'Camino de la Mula' },
];

export default function InvestigatorScreen({ searchResults = null }) {
  const [activePattern, setActivePattern] = useState('none');
  const [elements, setElements] = useState({ nodes: [], edges: [] });
  const [graphLayout, setGraphLayout] = useState('cose');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedNode, setSelectedNode] = useState(null);
  const [nodeDetails, setNodeDetails] = useState(null);
  const [blocking, setBlocking] = useState(false);

  // ─── Dynamic selectors ────────────────────────────────────────────────────
  const [origenesList, setOrigenesList] = useState([]);
  const [destinosList, setDestinosList] = useState([]);
  const [ciclosList, setCiclosList] = useState([]);
  const [origen, setOrigen] = useState('c04');
  const [destino, setDestino] = useState('c10');
  const [cuentaCiclo, setCuentaCiclo] = useState('c01');

  // Fetch filtered lists for selectors on mount
  useEffect(() => {
    apiClient.get('/fraude/cuentas/origenes-path')
      .then((res) => {
        const data = res.data || [];
        setOrigenesList(data);
        if (data.length > 0) setOrigen(data[0].id);
      })
      .catch(() => {});
    apiClient.get('/fraude/cuentas/destinos-path')
      .then((res) => {
        const data = res.data || [];
        setDestinosList(data);
        if (data.length > 0) setDestino(data[0].id);
      })
      .catch(() => {});
    apiClient.get('/fraude/cuentas/con-ciclos')
      .then((res) => {
        const data = res.data || [];
        setCiclosList(data);
        if (data.length > 0) setCuentaCiclo(data[0].id);
      })
      .catch(() => {});
  }, []);

  // ─── Pattern loaders ──────────────────────────────────────────────────────

  const loadIdentidadSintetica = useCallback(async () => {
    setLoading(true);
    setError(null);
    setActivePattern('identidad-sintetica');
    setSelectedNode(null);
    setNodeDetails(null);

    try {
      const res = await apiClient.get('/fraude/personas/identidad-sintetica', {
        params: { minPersonasVinculadas: 2 },
      });
      let result = { nodes: [], edges: [] };
      if (res.data && res.data.length > 0) {
        // Collect all unique persona IDs across all results
        const allPersonaIds = new Set();
        res.data.forEach((r) => {
          allPersonaIds.add(r.personaId);
          (r.personasRelacionadasIds || []).forEach((id) => allPersonaIds.add(id));
        });

        // Fetch devices for all personas in parallel
        const devicesMap = {};
        await Promise.all(
          [...allPersonaIds].map(async (pId) => {
            try {
              const devRes = await apiClient.get(`/dispositivos/persona/${pId}`);
              devicesMap[pId] = devRes.data || [];
            } catch {
              devicesMap[pId] = [];
            }
          })
        );

        const allResults = res.data.map((r) => mapIdentidadSinteticaToCytoscape(r, devicesMap));
        const allNodes = allResults.flatMap((r) => r.nodes);
        const allEdges = allResults.flatMap((r) => r.edges);
        const uniqueNodes = [...new Map(allNodes.map((n) => [n.data.id, n])).values()];
        const uniqueEdges = [...new Map(allEdges.map((e) => [e.data.id, e])).values()];
        result = { nodes: uniqueNodes, edges: uniqueEdges };
        setGraphLayout('radial');
      }
      setElements(result);
    } catch (err) {
      setError(`Error al cargar identidad sintética: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  }, []);

  const handlePatternClick = useCallback(
    (patternId) => {
      if (patternId === 'identidad-sintetica') {
        loadIdentidadSintetica();
      } else {
        // For shortest-path and ciclos, just set the active pattern (shows the controls)
        setActivePattern(patternId);
        setElements({ nodes: [], edges: [] });
        setSelectedNode(null);
        setNodeDetails(null);
        setError(null);
      }
    },
    [loadIdentidadSintetica]
  );

  // ─── Shortest Path: Analizar Ruta ─────────────────────────────────────────

  const handleAnalizarRuta = useCallback(async () => {
    setLoading(true);
    setError(null);
    setSelectedNode(null);
    setNodeDetails(null);

    try {
      const res = await apiClient.get('/grafo/cuentas/shortest-path', {
        params: { origen, destino },
      });
      let result = { nodes: [], edges: [] };
      if (res.data) {
        try {
          result = mapShortestPathToCytoscape(res.data);
        } catch (mapErr) {
          console.error('Error al mapear shortest-path a Cytoscape:', mapErr);
          setError('Error al procesar los datos del camino. Revisá la consola para más detalle.');
          return;
        }
        setGraphLayout('cose');
      }
      setElements(result);
    } catch (err) {
      setError(`Error al analizar ruta: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  }, [origen, destino]);

  // ─── Ciclos: Analizar Ciclos ──────────────────────────────────────────────

  const handleAnalizarCiclos = useCallback(async () => {
    setLoading(true);
    setError(null);
    setSelectedNode(null);
    setNodeDetails(null);

    try {
      const res = await apiClient.get(`/grafo/cuentas/${cuentaCiclo}/ciclos`, {
        params: { limite: 5 },
      });
      let result = { nodes: [], edges: [] };
      if (res.data && res.data.length > 0) {
        const allCuentasRes = await apiClient.get('/cuentas');
        result = mapCicloToCytoscape(res.data[0], allCuentasRes.data);
        setGraphLayout('circular');
      }
      setElements(result);
    } catch (err) {
      setError(`Error al cargar ciclos: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  }, [cuentaCiclo]);

  // ─── Search results ───────────────────────────────────────────────────────

  useEffect(() => {
    if (!searchResults) return;

    const loadSearch = async () => {
      setLoading(true);
      setError(null);
      setActivePattern('none');

      try {
        const nodes = [];
        const edges = [];

        if (searchResults.personas?.length > 0) {
          const persona = searchResults.personas[0];
          nodes.push({
            data: { id: persona.id, label: `${persona.nombre} ${persona.apellido}`, tipo: 'Persona' },
          });

          try {
            const cuentasRes = await apiClient.get(`/cuentas/persona/${persona.id}`);
            cuentasRes.data.forEach((c) => {
              nodes.push({ data: { id: c.id, label: c.alias || c.numeroCuenta, tipo: 'Cuenta', estado: c.estado } });
              edges.push({ data: { id: `edge-${persona.id}-${c.id}`, source: persona.id, target: c.id, label: 'Posee' } });
            });
          } catch {}

          try {
            const dispRes = await apiClient.get(`/dispositivos/persona/${persona.id}`);
            dispRes.data.forEach((d) => {
              nodes.push({ data: { id: d.id, label: d.fingerprint, tipo: 'Dispositivo' } });
              edges.push({ data: { id: `edge-${persona.id}-${d.id}`, source: persona.id, target: d.id, label: 'Usa' } });
            });
          } catch {}

          setGraphLayout('cose');
        } else if (searchResults.cuentas?.length > 0) {
          const cuenta = searchResults.cuentas[0];
          nodes.push({
            data: { id: cuenta.id, label: cuenta.alias || cuenta.numeroCuenta, tipo: 'Cuenta', estado: cuenta.estado },
            classes: cuenta.estado === 'BLOQUEADA' ? 'bloqueada' : '',
          });
          setGraphLayout('cose');
        } else if (searchResults.dispositivos?.length > 0) {
          const disp = searchResults.dispositivos[0];
          nodes.push({
            data: { id: disp.id, label: disp.fingerprint, tipo: 'Dispositivo' },
            classes: disp.esSospechoso ? 'sospechoso' : '',
          });
          setGraphLayout('cose');
        }

        const uniqueNodes = [...new Map(nodes.map((n) => [n.data.id, n])).values()];
        const uniqueEdges = [...new Map(edges.map((e) => [e.data.id, e])).values()];
        setElements({ nodes: uniqueNodes, edges: uniqueEdges });
      } catch (err) {
        setError('Error al procesar la búsqueda');
      } finally {
        setLoading(false);
      }
    };

    loadSearch();
  }, [searchResults]);

  // ─── Node selection & details ──────────────────────────────────────────────

  const handleNodeSelect = useCallback(async (node) => {
    setSelectedNode(node);

    try {
      const tipo = node.data.tipo;
      let details = null;

      if (tipo === 'Persona' || tipo === 'persona') {
        const res = await apiClient.get(`/personas/${node.id}`);
        details = { tipo: 'persona', ...res.data };
      } else if (tipo === 'Cuenta' || tipo === 'cuenta' || tipo === 'ciclo') {
        const res = await apiClient.get(`/cuentas/${node.id}`);
        details = { tipo: 'cuenta', ...res.data };
      } else if (tipo === 'Dispositivo' || tipo === 'dispositivo') {
        const res = await apiClient.get(`/dispositivos/${node.id}`);
        details = { tipo: 'dispositivo', ...res.data };
      }

      setNodeDetails(details);
    } catch (err) {
      setNodeDetails(null);
    }
  }, []);

  const handleBlockToggle = useCallback(async () => {
    if (!nodeDetails || nodeDetails.tipo !== 'cuenta') return;
    setBlocking(true);

    try {
      const nuevoEstado = nodeDetails.estado === 'BLOQUEADA' ? 'ACTIVA' : 'BLOQUEADA';
      await apiClient.put(`/cuentas/${nodeDetails.id}`, {
        ...nodeDetails,
        estado: nuevoEstado,
      });

      setNodeDetails((prev) => ({ ...prev, estado: nuevoEstado }));

      setElements((prev) => ({
        nodes: prev.nodes.map((n) =>
          n.data.id === nodeDetails.id
            ? {
                ...n,
                data: { ...n.data, estado: nuevoEstado },
                classes: nuevoEstado === 'BLOQUEADA' ? 'bloqueada' : '',
              }
            : n
        ),
        edges: prev.edges,
      }));
    } catch (err) {
      setError('Error al actualizar el estado de la cuenta');
    } finally {
      setBlocking(false);
    }
  }, [nodeDetails]);

  const handleRemoveLink = useCallback(async (relType, targetId) => {
    if (!nodeDetails) return;
    try {
      setElements((prev) => ({
        nodes: prev.nodes.filter((n) => n.data.id !== targetId),
        edges: prev.edges.filter(
          (e) => !(e.data.source === nodeDetails.id && e.data.target === targetId)
        ),
      }));
    } catch (err) {
      setError('Error al desvincular');
    }
  }, [nodeDetails]);

  // ─── Shared select style ──────────────────────────────────────────────────

  const selectClass =
    'rounded-md border border-slate-300 bg-white px-2 py-1 text-xs text-slate-700 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500';

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col h-[calc(100vh-80px)]">
      <div className="mb-4">
        <h1 className="text-xl font-semibold text-slate-800">Centro de Investigación Visual</h1>
        <p className="text-sm text-slate-500 mt-1">
          Explorá patrones de fraude y conexiones en el grafo interactivo
        </p>
      </div>

      {/* ─── Pattern buttons + dynamic controls ────────────────────────────── */}
      <div className="flex items-center gap-2 mb-4 flex-wrap">
        {PATRONES.map((p) => (
          <Button
            key={p.id}
            variant={activePattern === p.id ? 'primary' : 'outline'}
            size="sm"
            onClick={() => handlePatternClick(p.id)}
            loading={loading && activePattern === p.id}
          >
            {p.label}
          </Button>
        ))}
        {activePattern !== 'none' && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setActivePattern('none');
              setElements({ nodes: [], edges: [] });
              setNodeDetails(null);
              setSelectedNode(null);
            }}
          >
            Limpiar
          </Button>
        )}

        {/* ─── Shortest Path controls ────────────────────────────────────── */}
        {activePattern === 'shortest-path' && (
          <div className="flex items-center gap-2 ml-2 pl-2 border-l border-slate-200">
            <label className="text-xs text-slate-500">Origen:</label>
            <select className={selectClass} value={origen} onChange={(e) => setOrigen(e.target.value)}>
              {origenesList.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.alias || c.id}
                </option>
              ))}
            </select>
            <label className="text-xs text-slate-500">Destino:</label>
            <select className={selectClass} value={destino} onChange={(e) => setDestino(e.target.value)}>
              {destinosList.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.alias || c.id}
                </option>
              ))}
            </select>
            <Button size="sm" onClick={handleAnalizarRuta} loading={loading}>
              Analizar Ruta
            </Button>
          </div>
        )}

        {/* ─── Ciclos controls ───────────────────────────────────────────── */}
        {activePattern === 'lavado-circular' && (
          <div className="flex items-center gap-2 ml-2 pl-2 border-l border-slate-200">
            <label className="text-xs text-slate-500">Cuenta:</label>
            <select className={selectClass} value={cuentaCiclo} onChange={(e) => setCuentaCiclo(e.target.value)}>
              {ciclosList.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.alias || c.id}
                </option>
              ))}
            </select>
            <Button size="sm" onClick={handleAnalizarCiclos} loading={loading}>
              Analizar Ciclos
            </Button>
          </div>
        )}
      </div>

      <div className="flex-1 flex gap-4 min-h-0">
        <div className="flex-1 relative">
          <Card className="h-full p-0 overflow-hidden relative">
            {loading ? (
              <div className="flex items-center justify-center h-full">
                <Spinner size="lg" />
              </div>
            ) : error ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <p className="text-sm text-danger-600 mb-2">{error}</p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      if (activePattern === 'shortest-path') handleAnalizarRuta();
                      else if (activePattern === 'lavado-circular') handleAnalizarCiclos();
                      else if (activePattern === 'identidad-sintetica') loadIdentidadSintetica();
                    }}
                  >
                    Reintentar
                  </Button>
                </div>
              </div>
            ) : elements.nodes.length === 0 ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <Network className="h-12 w-12 text-slate-300 mx-auto mb-3" />
                  <p className="text-sm text-slate-500 mb-1">El lienzo está vacío</p>
                  <p className="text-xs text-slate-400">
                    Seleccioná un patrón de fraude o usá el buscador para comenzar
                  </p>
                </div>
              </div>
            ) : (
              <GraphCanvas
                elements={elements}
                layout={graphLayout}
                onNodeSelect={handleNodeSelect}
              />
            )}
            <GraphLegend />
          </Card>
        </div>

        <div className="w-80 flex flex-col gap-4 overflow-y-auto">
          {nodeDetails ? (
            <Card>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-slate-800">
                  {nodeDetails.tipo === 'persona'
                    ? 'Perfil de Persona'
                    : nodeDetails.tipo === 'cuenta'
                    ? 'Detalle de Cuenta'
                    : 'Dispositivo'}
                </h3>
                <Badge
                  variant={
                    nodeDetails.estado === 'BLOQUEADA'
                      ? 'danger'
                      : nodeDetails.nivelRiesgo >= 70
                      ? 'danger'
                      : nodeDetails.nivelRiesgo >= 40
                      ? 'warning'
                      : 'success'
                  }
                  size="sm"
                  dot
                >
                  {nodeDetails.estado === 'BLOQUEADA'
                    ? 'Bloqueada'
                    : nodeDetails.esSospechoso
                    ? 'Sospechoso'
                    : nodeDetails.esPEP
                    ? 'PEP'
                    : nodeDetails.esSancionado
                    ? 'Sancionado'
                    : 'Normal'}
                </Badge>
              </div>

              {nodeDetails.tipo === 'persona' && (
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Nombre</span>
                    <span className="text-slate-800 font-medium">
                      {nodeDetails.nombre} {nodeDetails.apellido}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">DNI</span>
                    <span className="text-slate-800 font-mono">{nodeDetails.dni}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Riesgo</span>
                    <span className="text-slate-800">{nodeDetails.nivelRiesgo}/100</span>
                  </div>
                  {nodeDetails.esPEP && (
                    <div className="flex justify-between">
                      <span className="text-slate-500">PEP</span>
                      <Badge variant="warning" size="sm">Sí</Badge>
                    </div>
                  )}
                  {nodeDetails.esSancionado && (
                    <div className="flex justify-between">
                      <span className="text-slate-500">Sancionado</span>
                      <Badge variant="danger" size="sm">Sí</Badge>
                    </div>
                  )}
                </div>
              )}

              {nodeDetails.tipo === 'cuenta' && (
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-500">Alias</span>
                    <span className="text-slate-800 font-medium">{nodeDetails.alias}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Banco</span>
                    <span className="text-slate-800">{nodeDetails.banco}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Cuenta</span>
                    <span className="text-slate-800 font-mono">{nodeDetails.numeroCuenta}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Saldo</span>
                    <span className="text-slate-800">{formatCurrency(nodeDetails.saldo)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-slate-500">Bloqueo preventivo</span>
                    <Switch
                      checked={nodeDetails.estado === 'BLOQUEADA'}
                      onChange={handleBlockToggle}
                      disabled={blocking}
                    />
                  </div>
                </div>
              )}

              {nodeDetails.tipo === 'dispositivo' && (
                <div className="space-y-2 text-xs">
                  {nodeDetails.marca && (
                    <div className="flex justify-between">
                      <span className="text-slate-500">Marca / Modelo</span>
                      <span className="text-slate-800 font-medium">
                        {nodeDetails.marca} {nodeDetails.modelo || ''}
                      </span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-slate-500">Fingerprint</span>
                    <span className="text-slate-800 font-mono truncate max-w-[120px]">{nodeDetails.fingerprint}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">IP</span>
                    <span className="text-slate-800 font-mono">{nodeDetails.ipAddress}</span>
                  </div>
                  {nodeDetails.ipPais && (
                    <div className="flex justify-between">
                      <span className="text-slate-500">Ubicación</span>
                      <span className="text-slate-800">{nodeDetails.ipPais}</span>
                    </div>
                  )}
                  {nodeDetails.tipoDispositivo && (
                    <div className="flex justify-between">
                      <span className="text-slate-500">Tipo</span>
                      <span className="text-slate-800">{nodeDetails.tipoDispositivo}</span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-slate-500">Personas asociadas</span>
                    <span className="text-slate-800">{nodeDetails.cantidadPersonasAsociadas ?? '—'}</span>
                  </div>
                  {(nodeDetails.esEmulador || nodeDetails.ipEsTor || nodeDetails.ipEsVPN || nodeDetails.esRooteado || nodeDetails.esSospechoso) && (
                    <div className="pt-2 border-t border-slate-100">
                      <p className="text-slate-500 mb-1.5">Redes de Riesgo</p>
                      <div className="flex flex-wrap gap-1.5">
                        {nodeDetails.esEmulador && <Badge variant="danger" size="sm">Emulador</Badge>}
                        {nodeDetails.esRooteado && <Badge variant="danger" size="sm">Rooteado</Badge>}
                        {nodeDetails.ipEsTor && <Badge variant="danger" size="sm">Tor</Badge>}
                        {nodeDetails.ipEsVPN && <Badge variant="warning" size="sm">VPN</Badge>}
                        {nodeDetails.esSospechoso && <Badge variant="danger" size="sm">Sospechoso</Badge>}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </Card>
          ) : (
            <Card className="bg-slate-50">
              <div className="text-center py-4">
                <p className="text-sm text-slate-500">
                  Hacé clic en un nodo del grafo para ver sus detalles
                </p>
              </div>
            </Card>
          )}
        </div>
      </div>

      <div className="mt-4">
        <DidacticPanel pattern={activePattern} />
      </div>
    </div>
  );
}
