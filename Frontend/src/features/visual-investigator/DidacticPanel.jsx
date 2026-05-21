import Card from '../../components/ui/Card';
import Badge from '../../components/ui/Badge';

const PATTERNS = {
  'lavado-circular': {
    title: 'Lavado Circular (Layering)',
    icon: '🔄',
    description:
      'Imaginá que alguien pasa dinero de una cuenta a otra, y de esa a otra, y así sucesivamente, hasta que vuelve a la cuenta original. Es como una ronda donde el dinero da vueltas para confundir su origen. El sistema detecta estos círculos cerrados de transferencias automáticamente.',
    badge: 'danger',
  },
  'identidad-sintetica': {
    title: 'Identidad Sintética',
    icon: '👤',
    description:
      'Un delincuente crea varias identidades falsas usando datos reales mezclados con inventados (por ejemplo, un DNI real con un nombre falso). Todas estas "personas" son operadas desde el mismo dispositivo. El sistema detecta cuando múltiples identidades comparten el mismo equipo.',
    badge: 'warning',
  },
  'shortest-path': {
    title: 'Camino de la Mula',
    icon: '➡️',
    description:
      'El dinero robado no va directo al delincuente. Pasa por varias cuentas intermedias llamadas "mulas" para dificultar el rastreo. El sistema encuentra el camino más corto entre dos cuentas sospechosas, revelando toda la cadena de intermediarios.',
    badge: 'warning',
  },
  'account-takeover': {
    title: 'Robo de Cuenta (Account Takeover)',
    icon: '🔓',
    description:
      'Un delincuente accede a cuentas de otras personas usando un dispositivo comprometido. Cuando varias cuentas diferentes son operadas desde el mismo equipo, es una señal de que alguien tomó control de esas cuentas.',
    badge: 'danger',
  },
  'smurfing': {
    title: 'Smurfing (Fragmentación)',
    icon: '🧩',
    description:
      'Para evadir controles, el dinero se envía en montos pequeños desde muchas cuentas distintas hacia una sola cuenta destino. Es como gotas de agua que juntas forman un río. El sistema detecta cuando una cuenta recibe muchas transferencias pequeñas en poco tiempo.',
    badge: 'warning',
  },
  none: {
    title: 'Seleccioná un patrón',
    icon: '🔍',
    description:
      'Elegí un patrón de fraude del menú superior para visualizarlo en el grafo. Cada patrón muestra una técnica diferente que los delincuentes usan para mover dinero o evadir controles.',
    badge: 'neutral',
  },
};

export default function DidacticPanel({ pattern = 'none' }) {
  const info = PATTERNS[pattern] || PATTERNS.none;

  return (
    <Card className="bg-slate-50 border-slate-200">
      <div className="flex items-start gap-3">
        <span className="text-2xl">{info.icon}</span>
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <h4 className="text-sm font-semibold text-slate-800">{info.title}</h4>
            <Badge variant={info.badge} size="sm">
              {pattern !== 'none' ? 'Patrón detectado' : 'Info'}
            </Badge>
          </div>
          <p className="text-xs text-slate-600 leading-relaxed">{info.description}</p>
        </div>
      </div>
    </Card>
  );
}
