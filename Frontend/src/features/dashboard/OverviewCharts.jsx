import { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import Card from '../../components/ui/Card';
import Spinner from '../../components/ui/Spinner';
import apiClient from '../../services/apiClient';

const COLORS = ['#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'];

export default function OverviewCharts() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await apiClient.get('/fraude/dispositivos/multi-cuenta');
        const formatted = response.data.map((d) => ({
          name: d.fingerprint?.substring(0, 16) || d.dispositivoId,
          titulares: d.totalTitulares || 0,
          cuentas: d.totalCuentas || 0,
          id: d.dispositivoId,
          esEmulador: d.esEmulador,
          ipEsTor: d.ipEsTor,
          ipEsVPN: d.ipEsVPN,
        }));
        setData(formatted);
      } catch (err) {
        setError('No se pudieron cargar los datos de dispositivos multicuenta.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <Card>
        <div className="flex items-center justify-center h-64">
          <Spinner />
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <div className="flex items-center justify-center h-64">
          <p className="text-sm text-slate-500">{error}</p>
        </div>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <Card>
        <h3 className="text-base font-semibold text-slate-800 mb-1">Alertas por Dispositivos Multicuenta</h3>
        <p className="text-xs text-slate-500 mb-4">
          Cantidad de personas distintas que comparten un mismo dispositivo físico
        </p>

        {data.length === 0 ? (
          <div className="flex items-center justify-center h-48">
            <p className="text-sm text-slate-400">No se detectaron dispositivos multicuenta</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis
                dataKey="name"
                tick={{ fontSize: 11, fill: '#64748b' }}
                axisLine={{ stroke: '#e2e8f0' }}
              />
              <YAxis
                tick={{ fontSize: 11, fill: '#64748b' }}
                axisLine={{ stroke: '#e2e8f0' }}
                allowDecimals={false}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#fff',
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  fontSize: '12px',
                }}
                formatter={(value, name) => [
                  value,
                  name === 'titulares' ? 'Personas distintas' : 'Cuentas vinculadas',
                ]}
              />
              <Bar dataKey="titulares" radius={[4, 4, 0, 0]}>
                {data.map((entry, index) => (
                  <Cell
                    key={entry.id}
                    fill={entry.esEmulador || entry.ipEsTor ? '#ef4444' : entry.ipEsVPN ? '#f59e0b' : COLORS[index % COLORS.length]}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </Card>

      <Card className="bg-emerald-50 border-emerald-200">
        <div className="flex gap-3">
          <div className="text-emerald-600 text-lg">&#9432;</div>
          <div>
            <p className="text-sm font-medium text-emerald-800 mb-1">¿Por qué es importante?</p>
            <p className="text-xs text-emerald-700 leading-relaxed">
              Cuando múltiples personas utilizan el mismo dispositivo físico para operar, es una señal de alerta
              de fraude. Esto puede indicar que un delincuente está usando cuentas robadas o creadas con identidades
              falsas desde un solo equipo. Los dispositivos marcados en rojo son emuladores o usan red Tor, lo que
              agrava la sospecha.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
