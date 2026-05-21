import { useState } from 'react';
import { Send, CheckCircle, AlertTriangle } from 'lucide-react';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Badge from '../../components/ui/Badge';
import apiClient from '../../services/apiClient';

const TIPOS_TX = ['TRANSFERENCIA', 'DEPOSITO', 'RETIRO', 'PAGO'];
const CANALES = ['APP_MOVIL', 'HOME_BANKING', 'CAJERO', 'SUCURSAL'];
const ESTADOS = ['COMPLETADA', 'PENDIENTE', 'RECHAZADA'];

export default function SimulatorForm({ onSuccess }) {
  const [form, setForm] = useState({
    cuentaOrigenId: '',
    cuentaDestinoId: '',
    monto: '',
    tipo: 'TRANSFERENCIA',
    canal: 'APP_MOVIL',
    descripcion: '',
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setResult(null);
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.cuentaOrigenId || !form.cuentaDestinoId || !form.monto) {
      setError('Completá los campos obligatorios');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const payload = {
        cuentaOrigenId: form.cuentaOrigenId,
        cuentaDestinoId: form.cuentaDestinoId,
        monto: form.monto,
        moneda: 'ARS',
        tipo: form.tipo,
        canal: form.canal,
        descripcion: form.descripcion || 'Transacción simulada desde el SOC',
      };

      const res = await apiClient.post('/transacciones', payload);
      setResult(res.data);
      onSuccess?.(res.data);

      setForm((prev) => ({ ...prev, monto: '', descripcion: '' }));
    } catch (err) {
      setError(err.response?.data?.message || 'Error al crear la transacción');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <h3 className="text-base font-semibold text-slate-800 mb-1">Simulador de Eventos</h3>
      <p className="text-xs text-slate-500 mb-4">
        Inyectá una transacción para probar cómo responde el sistema de detección
      </p>

      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Cuenta Origen *</label>
          <input
            type="text"
            name="cuentaOrigenId"
            value={form.cuentaOrigenId}
            onChange={handleChange}
            placeholder="Ej: c01"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            required
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Cuenta Destino *</label>
          <input
            type="text"
            name="cuentaDestinoId"
            value={form.cuentaDestinoId}
            onChange={handleChange}
            placeholder="Ej: c02"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            required
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Monto *</label>
          <input
            type="number"
            name="monto"
            value={form.monto}
            onChange={handleChange}
            placeholder="50000"
            min="0"
            step="0.01"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            required
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Tipo</label>
            <select
              name="tipo"
              value={form.tipo}
              onChange={handleChange}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              {TIPOS_TX.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-600 mb-1">Canal</label>
            <select
              name="canal"
              value={form.canal}
              onChange={handleChange}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              {CANALES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Descripción</label>
          <input
            type="text"
            name="descripcion"
            value={form.descripcion}
            onChange={handleChange}
            placeholder="Motivo o concepto..."
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>

        <Button type="submit" loading={loading} className="w-full">
          <Send className="h-4 w-4" />
          Enviar Transacción
        </Button>
      </form>

      {result && (
        <div className="mt-4 p-3 rounded-lg bg-emerald-50 border border-emerald-200">
          <div className="flex items-center gap-2 mb-2">
            <CheckCircle className="h-4 w-4 text-emerald-600" />
            <span className="text-sm font-medium text-emerald-800">Transacción creada</span>
          </div>
          <div className="space-y-1 text-xs text-emerald-700">
            <p>ID: <span className="font-mono">{result.id}</span></p>
            <p>Orden: <span className="font-mono">{result.numeroOrden}</span></p>
            {result.esAlertada && (
              <Badge variant="danger" size="sm" dot>
                Alertada - {result.motivoAlerta}
              </Badge>
            )}
            {result.nivelRiesgo > 0 && (
              <p>Nivel de riesgo: <strong>{result.nivelRiesgo}/100</strong></p>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="mt-4 p-3 rounded-lg bg-danger-50 border border-danger-200">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-danger-600" />
            <span className="text-sm text-danger-800">{error}</span>
          </div>
        </div>
      )}
    </Card>
  );
}
