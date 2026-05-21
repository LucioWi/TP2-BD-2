const NODE_COLORS = {
  Persona: '#3b82f6',
  Cuenta: '#10b981',
  Dispositivo: '#6b7280',
  Transaccion: '#8b5cf6',
};

const NODE_SHAPES = {
  Persona: 'ellipse',
  Cuenta: 'round-rectangle',
  Dispositivo: 'diamond',
  Transaccion: 'hexagon',
};

function stripNulls(obj) {
  if (!obj || typeof obj !== 'object') return {};
  const result = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value !== null && value !== undefined) {
      result[key] = value;
    }
  }
  return result;
}

export function mapShortestPathToCytoscape(shortestPathDto) {
  if (!shortestPathDto?.elements) return { nodes: [], edges: [] };

  const rawNodes = shortestPathDto.elements.nodes || [];
  const rawEdges = shortestPathDto.elements.edges || [];

  const nodes = rawNodes
    .filter((n) => n?.data?.id)
    .map((node) => {
      const d = node.data;
      return {
        data: {
          id: d.id,
          label: d.label || d.numeroCuenta || d.id,
          tipo: d.esOrigen ? 'origen' : d.esDestino ? 'destino' : d.esIntermediario ? 'intermediario' : 'normal',
          ...stripNulls(d),
        },
        classes: getNodeClasses(d),
      };
    });

  const edges = rawEdges
    .filter((e) => e?.data?.source && e?.data?.target)
    .map((edge, i) => {
      const d = edge.data;
      const src = d.source;
      const tgt = d.target;
      const monto = d.monto;
      return {
        data: {
          id: `tx-${src}-${tgt}-${i}`,
          source: src,
          target: tgt,
          label: monto ? `$${parseFloat(monto).toLocaleString('es-AR')}` : '',
          ...stripNulls(d),
        },
      };
    });

  return { nodes, edges };
}

function getNodeClasses(nodeData) {
  if (!nodeData) return '';
  const classes = [];
  if (nodeData.esOrigen) classes.push('origen');
  if (nodeData.esDestino) classes.push('destino');
  if (nodeData.esIntermediario) classes.push('intermediario');
  if (nodeData.estado === 'BLOQUEADA') classes.push('bloqueada');
  if (nodeData.riesgoNodo >= 75) classes.push('alto-riesgo');
  return classes.join(' ');
}

export function mapCicloToCytoscape(cicloDto, allCuentas = []) {
  const nodes = [];
  const edges = [];
  const cuentasEnCiclo = cicloDto.cuentasEnCiclo || [];

  cuentasEnCiclo.forEach((cuentaId, index) => {
    const cuentaInfo = allCuentas.find((c) => c.id === cuentaId);
    nodes.push({
      data: {
        id: cuentaId,
        label: cuentaInfo?.alias || cuentaInfo?.numeroCuenta || cuentaId,
        tipo: 'ciclo',
        banco: cuentaInfo?.banco,
        estado: cuentaInfo?.estado,
      },
      classes: cuentaInfo?.estado === 'BLOQUEADA' ? 'bloqueada' : 'ciclo',
    });

    if (index > 0) {
      edges.push({
        data: {
          id: `edge-ciclo-${cuentasEnCiclo[index - 1]}-${cuentaId}`,
          source: cuentasEnCiclo[index - 1],
          target: cuentaId,
          tipo: 'ciclo',
        },
      });
    }
  });

  if (cuentasEnCiclo.length > 1) {
    edges.push({
      data: {
        id: `edge-ciclo-close-${cuentasEnCiclo[cuentasEnCiclo.length - 1]}-${cuentasEnCiclo[0]}`,
        source: cuentasEnCiclo[cuentasEnCiclo.length - 1],
        target: cuentasEnCiclo[0],
        tipo: 'ciclo',
      },
    });
  }

  return { nodes, edges };
}

export function mapIdentidadSinteticaToCytoscape(identidadDto, devicesMap = {}) {
  const nodes = [];
  const edges = [];

  // Create real device nodes from backend data
  const personaDevices = devicesMap[identidadDto.personaId] || [];
  personaDevices.forEach((dev) => {
    nodes.push({
      data: {
        id: dev.id,
        label: dev.marca && dev.modelo ? `${dev.marca} ${dev.modelo}` : dev.fingerprint || dev.id,
        tipo: 'Dispositivo',
        ...dev,
      },
      classes: `dispositivo${dev.esSospechoso ? ' sospechoso' : ''}`,
    });
  });

  // Create persona nodes and edges to devices
  const personasIds = identidadDto.personasRelacionadasIds || [];
  personasIds.forEach((pId) => {
    nodes.push({
      data: {
        id: pId,
        label: pId,
        tipo: 'Persona',
      },
      classes: 'persona',
    });

    // Edge from each persona to each device linked to the main persona
    personaDevices.forEach((dev) => {
      edges.push({
        data: {
          id: `edge-syn-${pId}-${dev.id}`,
          source: dev.id,
          target: pId,
          label: 'Compartido',
          tipo: 'identidad-sintetica',
        },
      });
    });
  });

  // Also add the main persona node
  if (identidadDto.nombre) {
    nodes.push({
      data: {
        id: identidadDto.personaId,
        label: `${identidadDto.nombre} ${identidadDto.apellido || ''}`.trim(),
        tipo: 'Persona',
      },
      classes: 'persona',
    });
    personaDevices.forEach((dev) => {
      edges.push({
        data: {
          id: `edge-syn-main-${identidadDto.personaId}-${dev.id}`,
          source: dev.id,
          target: identidadDto.personaId,
          label: 'Usa',
          tipo: 'identidad-sintetica',
        },
      });
    });
  }

  return { nodes, edges };
}

export function mapEntidadesToCytoscape(entidades, tipo = 'cuenta') {
  const nodes = entidades.map((entidad) => ({
    data: {
      id: entidad.id,
      label: entidad.alias || entidad.numeroCuenta || entidad.nombre || entidad.fingerprint || entidad.id,
      tipo: tipo,
      ...entidad,
    },
    classes: entidad.estado === 'BLOQUEADA' ? 'bloqueada' : tipo === 'dispositivo' && entidad.esSospechoso ? 'sospechoso' : '',
  }));

  return { nodes, edges: [] };
}

export { NODE_COLORS, NODE_SHAPES };
