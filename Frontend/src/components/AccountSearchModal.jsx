import { X, Building2, Landmark, Hash, DollarSign, Activity } from 'lucide-react';
import Badge from './ui/Badge';
import { formatCurrency } from '../utils/formatCurrency';

const estadoConfig = {
  ACTIVA: { variant: 'success', label: 'Activa' },
  INACTIVA: { variant: 'neutral', label: 'Inactiva' },
  BLOQUEADA: { variant: 'danger', label: 'Bloqueada' },
  SUSPENDIDA: { variant: 'warning', label: 'Suspendida' },
  CERRADA: { variant: 'info', label: 'Cerrada' },
};

export default function AccountSearchModal({ isOpen, onClose, account, error }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-md mx-4 overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100">
          <h3 className="text-base font-semibold text-slate-800">Resultado de búsqueda</h3>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="p-5">
          {error ? (
            <div className="flex items-center gap-3 p-4 rounded-xl bg-red-50 border border-red-200">
              <div className="flex-shrink-0 h-8 w-8 rounded-full bg-red-100 flex items-center justify-center">
                <X className="h-4 w-4 text-red-600" />
              </div>
              <p className="text-sm text-red-700 font-medium">Cuenta no encontrada</p>
            </div>
          ) : account ? (
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="flex-shrink-0 h-10 w-10 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-center">
                  <Landmark className="h-5 w-5 text-emerald-600" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-800">{account.alias}</p>
                  <p className="text-xs text-slate-500">Alias de la cuenta</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div className="flex items-center gap-1.5 mb-1">
                    <Building2 className="h-3.5 w-3.5 text-slate-400" />
                    <span className="text-xs text-slate-500">Banco</span>
                  </div>
                  <p className="text-sm font-medium text-slate-700">{account.banco}</p>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div className="flex items-center gap-1.5 mb-1">
                    <Activity className="h-3.5 w-3.5 text-slate-400" />
                    <span className="text-xs text-slate-500">Estado</span>
                  </div>
                  {(() => {
                    const cfg = estadoConfig[account.estado] || estadoConfig.ACTIVA;
                    return <Badge variant={cfg.variant} dot>{cfg.label}</Badge>;
                  })()}
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div className="flex items-center gap-1.5 mb-1">
                    <DollarSign className="h-3.5 w-3.5 text-slate-400" />
                    <span className="text-xs text-slate-500">Saldo</span>
                  </div>
                  <p className="text-sm font-semibold text-slate-800">
                    {formatCurrency(account.saldo, account.moneda)}
                  </p>
                </div>

                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                  <div className="flex items-center gap-1.5 mb-1">
                    <Hash className="h-3.5 w-3.5 text-slate-400" />
                    <span className="text-xs text-slate-500">Nro Cuenta</span>
                  </div>
                  <p className="text-xs font-mono text-slate-600 truncate">{account.numeroCuenta}</p>
                </div>
              </div>
            </div>
          ) : null}
        </div>

        <div className="px-5 py-3 border-t border-slate-100 bg-slate-50/50">
          <button
            onClick={onClose}
            className="w-full py-2 text-sm font-medium text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}
