import { useEffect, useRef, useCallback, useState } from 'react';
import CytoscapeComponent from 'react-cytoscapejs';

const STYLESHEET = [
  {
    selector: 'node',
    style: {
      label: 'data(label)',
      'font-size': '11px',
      'font-family': 'Inter, sans-serif',
      'text-valign': 'bottom',
      'text-margin-y': 5,
      'text-wrap': 'ellipsis',
      'text-max-width': '80px',
      color: '#334155',
      'background-opacity': 0.9,
      'border-width': 2,
      'border-opacity': 0.8,
    },
  },
  {
    selector: 'node[?tipo]',
    style: {
      width: 40,
      height: 40,
    },
  },
  {
    selector: 'node[tipo="Persona"], node[tipo="persona"]',
    style: {
      'background-color': '#3b82f6',
      'border-color': '#2563eb',
      shape: 'ellipse',
    },
  },
  {
    selector: 'node[tipo="Cuenta"], node[tipo="cuenta"], node[tipo="ciclo"]',
    style: {
      'background-color': '#10b981',
      'border-color': '#059669',
      shape: 'round-rectangle',
      width: 50,
      height: 35,
    },
  },
  {
    selector: 'node[tipo="Dispositivo"], node[tipo="dispositivo"]',
    style: {
      'background-color': '#6b7280',
      'border-color': '#4b5563',
      shape: 'diamond',
      width: 40,
      height: 40,
    },
  },
  {
    selector: 'node.bloqueada',
    style: {
      'background-color': '#9ca3af',
      'border-color': '#6b7280',
      opacity: 0.6,
      label: 'data(label)',
      'background-image': 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>',
      'background-fit': 'none',
      'background-width': '16px',
      'background-height': '16px',
    },
  },
  {
    selector: 'node.origen',
    style: {
      'background-color': '#3b82f6',
      'border-color': '#1d4ed8',
      'border-width': 3,
      width: 45,
      height: 45,
    },
  },
  {
    selector: 'node.destino',
    style: {
      'background-color': '#ef4444',
      'border-color': '#dc2626',
      'border-width': 3,
      width: 45,
      height: 45,
    },
  },
  {
    selector: 'node.intermediario',
    style: {
      'background-color': '#f59e0b',
      'border-color': '#d97706',
      'border-width': 2,
    },
  },
  {
    selector: 'node.alto-riesgo',
    style: {
      'background-color': '#ef4444',
      'border-color': '#b91c1c',
    },
  },
  {
    selector: 'node.sospechoso',
    style: {
      'background-color': '#ef4444',
      'border-color': '#dc2626',
      'border-style': 'double',
    },
  },
  {
    selector: 'edge[label]',
    style: {
      'label': 'data(label)',
    }
  },
  {
    selector: 'edge',
    style: {
      width: 2,
      'line-color': '#cbd5e1',
      'target-arrow-color': '#cbd5e1',
      'target-arrow-shape': 'triangle',
      'arrow-scale': 0.8,
      'curve-style': 'bezier',
      'font-size': '10px',
      'font-family': 'Inter, sans-serif',
      color: '#64748b',
      'text-background-color': '#ffffff',
      'text-background-opacity': 0.8,
      'text-background-padding': '2px',
      'text-wrap': 'ellipsis',
      'text-max-width': '60px',
    },
  },
  {
    selector: 'edge[tipo="ciclo"]',
    style: {
      'line-color': '#ef4444',
      'target-arrow-color': '#ef4444',
      width: 2.5,
      'line-style': 'dashed',
    },
  },
  {
    selector: 'edge.highlighted',
    style: {
      'line-color': '#fbbf24',
      'target-arrow-color': '#fbbf24',
      width: 4,
      'z-index': 10,
    },
  },
  {
    selector: 'node.highlighted',
    style: {
      'background-color': '#fbbf24',
      'border-color': '#f59e0b',
      'border-width': 3,
      'z-index': 10,
    },
  },
  {
    selector: 'node:selected',
    style: {
      'border-width': 3,
      'border-color': '#10b981',
      'background-opacity': 1,
    },
  },
];

const DEFAULT_LAYOUT = {
  name: 'cose',
  animate: true,
  animationDuration: 500,
  nodeRepulsion: function () { return 4500; },
  idealEdgeLength: function () { return 150; },
  nodeOverlap: 20,
  gravity: 0.25,
  numIter: 1000,
  fit: true,
  padding: 30,
  randomize: false,
};

const CIRCULAR_LAYOUT = {
  name: 'circle',
  animate: true,
  animationDuration: 500,
  padding: 30,
};

const RADIAL_LAYOUT = {
  name: 'concentric',
  animate: true,
  animationDuration: 500,
  minNodeSpacing: 60,
};

export default function GraphCanvas({
  elements = [],
  layout = 'cose',
  onNodeSelect,
  highlightedEdges = [],
  className = '',
}) {
  const cyRef = useRef(null);
  const [renderError, setRenderError] = useState(null);

  const getLayout = useCallback(() => {
    switch (layout) {
      case 'circular':
        return CIRCULAR_LAYOUT;
      case 'radial':
        return RADIAL_LAYOUT;
      default:
        return DEFAULT_LAYOUT;
    }
  }, [layout]);

  useEffect(() => {
    if (!cyRef.current) return;
    const cy = cyRef.current;

    cy.elements().removeClass('highlighted');
    highlightedEdges.forEach((edgeId) => {
      cy.getElementById(edgeId).addClass('highlighted');
      const edge = cy.getElementById(edgeId);
      if (edge.source) cy.getElementById(edge.source().id()).addClass('highlighted');
      if (edge.target) cy.getElementById(edge.target().id()).addClass('highlighted');
    });
  }, [highlightedEdges]);

  useEffect(() => {
    if (!cyRef.current) return;
    const cy = cyRef.current;

    const handleTap = (evt) => {
      const node = evt.target;
      if (node.isNode && node.isNode()) {
        onNodeSelect?.({
          id: node.id(),
          data: node.data(),
        });
      }
    };

    cy.on('tap', 'node', handleTap);
    return () => {
      cy.off('tap', 'node', handleTap);
    };
  }, [onNodeSelect]);

  let normalizedElements;
  try {
    normalizedElements = CytoscapeComponent.normalizeElements(elements);
  } catch (e) {
    console.error('Error al normalizar elementos de Cytoscape:', e);
    normalizedElements = [];
  }

  return (
    <div className={`relative w-full h-full ${className}`}>
      {renderError ? (
        <div className="flex items-center justify-center h-full">
          <div className="text-center">
            <p className="text-sm text-red-600 mb-2">Error al renderizar el grafo</p>
            <p className="text-xs text-slate-500">{renderError}</p>
          </div>
        </div>
      ) : (
        <CytoscapeComponent
          elements={normalizedElements}
          stylesheet={STYLESHEET}
          layout={getLayout()}
          cy={(cy) => {
            try {
              cyRef.current = cy;
            } catch (e) {
              console.error('Error al inicializar Cytoscape:', e);
              setRenderError(e.message);
            }
          }}
          style={{ width: '100%', height: '100%' }}
          minZoom={0.3}
          maxZoom={3}
        />
      )}
    </div>
  );
}
