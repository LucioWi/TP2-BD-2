import Card from './ui/Card';

export default function KpiCard({ title, value, subtitle, icon: Icon, variant = 'default', loading = false }) {
  const borderColors = {
    default: 'border-l-slate-300',
    success: 'border-l-emerald-500',
    danger: 'border-l-danger-500',
    warning: 'border-l-warning-500',
    info: 'border-l-blue-500',
  };

  const iconColors = {
    default: 'text-slate-400',
    success: 'text-emerald-500',
    danger: 'text-danger-500',
    warning: 'text-warning-500',
    info: 'text-blue-500',
  };

  if (loading) {
    return (
      <Card className={`border-l-4 ${borderColors[variant]}`}>
        <div className="animate-pulse space-y-3">
          <div className="h-3 bg-slate-200 rounded w-3/4" />
          <div className="h-7 bg-slate-200 rounded w-1/2" />
          <div className="h-3 bg-slate-200 rounded w-full" />
        </div>
      </Card>
    );
  }

  return (
    <Card className={`border-l-4 ${borderColors[variant]}`}>
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">{title}</p>
          <p className="text-2xl font-semibold text-slate-800">{value}</p>
        </div>
        {Icon && (
          <div className={`p-2 rounded-lg bg-slate-50 ${iconColors[variant]}`}>
            <Icon className="h-5 w-5" />
          </div>
        )}
      </div>
      {subtitle && (
        <p className="mt-3 text-xs text-slate-500 leading-relaxed">{subtitle}</p>
      )}
    </Card>
  );
}
