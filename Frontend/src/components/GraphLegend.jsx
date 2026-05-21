export default function GraphLegend() {
  const items = [
    { color: 'bg-blue-500', label: 'Persona', shape: 'rounded-full' },
    { color: 'bg-emerald-500', label: 'Cuenta', shape: 'rounded-md' },
    { color: 'bg-slate-400', label: 'Dispositivo', shape: 'rotate-45 rounded-sm' },
    { color: 'bg-purple-500', label: 'Transacción', shape: 'rounded-lg' },
    { color: 'bg-danger-500', label: 'Alertado / Alto riesgo', shape: 'rounded-full' },
    { color: 'bg-warning-400', label: 'Camino resaltado', shape: 'rounded-full' },
  ];

  return (
    <div className="absolute bottom-4 left-4 bg-white/95 backdrop-blur-sm rounded-xl border border-slate-200 shadow-lg p-4 z-20">
      <p className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-3">Leyenda</p>
      <div className="space-y-2">
        {items.map((item) => (
          <div key={item.label} className="flex items-center gap-2.5">
            <div className={`w-3 h-3 ${item.color} ${item.shape} flex-shrink-0`} />
            <span className="text-xs text-slate-600">{item.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
