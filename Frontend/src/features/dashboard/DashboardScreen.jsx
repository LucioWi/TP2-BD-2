import { useState, useEffect } from 'react';
import { ShieldAlert, TrendingUp, AlertTriangle, DollarSign } from 'lucide-react';
import KpiCard from '../../components/KpiCard';
import OverviewCharts from './OverviewCharts';
import Card from '../../components/ui/Card';
import Badge from '../../components/ui/Badge';
import Spinner from '../../components/ui/Spinner';
import apiClient from '../../services/apiClient';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

export default function DashboardScreen() {
  const [stats, setStats] = useState({
    totalTx: 0,
    alertadas: 0,
    montoAlertado: 0,
    cuentasBloqueadas: 0,
  });
  const [alertasRecientes, setAlertasRecientes] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        setLoading(true);
        const [txRes, alertadasRes, bloqueadasRes] = await Promise.allSettled([
          apiClient.get('/transacciones'),
          apiClient.get('/fraude/transacciones/alertadas'),
          apiClient.get('/fraude/personas/cuentas-bloqueadas'),
        ]);

        const totalTx = txRes.status === 'fulfilled' ? txRes.value.data.length : 0;
        const alertadas = alertadasRes.status === 'fulfilled' ? alertadasRes.value.data : [];
        const montoAlertado = alertadas.reduce((sum, tx) => sum + parseFloat(tx.monto || 0), 0);
        const cuentasBloqueadas = bloqueadasRes.status === 'fulfilled' ? bloqueadasRes.value.data.length : 0;

        setStats({
          totalTx,
          alertadas: alertadas.length,
          montoAlertado,
          cuentasBloqueadas,
        });

        setAlertasRecientes(alertadas.slice(0, 5));
      } catch (err) {
        console.error('Error loading dashboard:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  const indiceRiesgo = stats.totalTx > 0 ? ((stats.alertadas / stats.totalTx) * 100).toFixed(1) : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Dashboard de Situación</h1>
        <p className="text-sm text-slate-500 mt-1">Vista ejecutiva global del estado de la red</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          title="Índice de Riesgo de la Red"
          value={`${indiceRiesgo}%`}
          subtitle="Mide el porcentaje de movimientos bajo análisis con comportamiento anómalo"
          icon={ShieldAlert}
          variant={parseFloat(indiceRiesgo) > 30 ? 'danger' : parseFloat(indiceRiesgo) > 15 ? 'warning' : 'success'}
          loading={loading}
        />
        <KpiCard
          title="Capital Bajo Análisis"
          value={formatCurrency(stats.montoAlertado)}
          subtitle="Suma de montos de transacciones críticas o alertadas"
          icon={DollarSign}
          variant="warning"
          loading={loading}
        />
        <KpiCard
          title="Transacciones Alertadas"
          value={stats.alertadas}
          subtitle={`De un total de ${stats.totalTx} transacciones registradas`}
          icon={AlertTriangle}
          variant={stats.alertadas > 10 ? 'danger' : 'warning'}
          loading={loading}
        />
        <KpiCard
          title="Cuentas Bloqueadas"
          value={stats.cuentasBloqueadas}
          subtitle="Cuentas con bloqueo preventivo activo"
          icon={TrendingUp}
          variant="info"
          loading={loading}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <OverviewCharts />
        </div>

        <Card>
          <h3 className="text-base font-semibold text-slate-800 mb-4">Alertas Recientes</h3>
          {loading ? (
            <Spinner className="py-8" />
          ) : alertasRecientes.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-8">No hay alertas activas</p>
          ) : (
            <div className="space-y-3">
              {alertasRecientes.map((tx) => (
                <div
                  key={tx.id}
                  className="flex items-start gap-3 p-3 rounded-lg bg-slate-50 border border-slate-100"
                >
                  <div className="mt-0.5">
                    <AlertTriangle className="h-4 w-4 text-danger-500" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-700 truncate">
                      {tx.motivoAlerta || 'Transacción sospechosa'}
                    </p>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xs text-slate-500">{formatCurrency(tx.monto)}</span>
                      <span className="text-xs text-slate-400">·</span>
                      <span className="text-xs text-slate-500">{formatDate(tx.fechaTransaccion)}</span>
                    </div>
                  </div>
                  <Badge variant="danger" size="sm" dot>
                    Riesgo {tx.nivelRiesgo}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
