import { useState, useEffect, useCallback } from 'react';
import { ClipboardList, Users, CreditCard, Smartphone, AlertTriangle, ChevronRight } from 'lucide-react';
import SimulatorForm from './SimulatorForm';
import EntityDetailsDrawer from './EntityDetailsDrawer';
import Card from '../../components/ui/Card';
import Badge from '../../components/ui/Badge';
import Spinner from '../../components/ui/Spinner';
import apiClient from '../../services/apiClient';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

const SECTIONS = [
  { id: 'personas', label: 'Personas', icon: Users },
  { id: 'cuentas', label: 'Cuentas', icon: CreditCard },
  { id: 'dispositivos', label: 'Dispositivos', icon: Smartphone },
  { id: 'alertas', label: 'Alertas', icon: AlertTriangle },
];

export default function OperationsScreen() {
  const [activeSection, setActiveSection] = useState('personas');
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedEntity, setSelectedEntity] = useState(null);

  const fetchData = useCallback(async (section) => {
    setLoading(true);
    setError(null);

    try {
      let res;
      switch (section) {
        case 'personas':
          res = await apiClient.get('/personas');
          break;
        case 'cuentas':
          res = await apiClient.get('/cuentas');
          break;
        case 'dispositivos':
          res = await apiClient.get('/dispositivos');
          break;
        case 'alertas':
          res = await apiClient.get('/fraude/transacciones/alertadas');
          break;
        default:
          res = { data: [] };
      }
      setData(res.data);
    } catch (err) {
      setError(`Error al cargar ${section}`);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(activeSection);
  }, [activeSection, fetchData]);

  const handleSimulatorSuccess = () => {
    if (activeSection === 'alertas') {
      fetchData('alertas');
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-800">Panel de Gestión Operativa</h1>
        <p className="text-sm text-slate-500 mt-1">
          Administrá entidades, simulá eventos y gestioná alertas
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center gap-2 overflow-x-auto pb-2">
            {SECTIONS.map((s) => (
              <button
                key={s.id}
                onClick={() => setActiveSection(s.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-all ${
                  activeSection === s.id
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                }`}
              >
                <s.icon className="h-4 w-4" />
                {s.label}
              </button>
            ))}
          </div>

          <Card padding={false}>
            {loading ? (
              <div className="flex items-center justify-center py-16">
                <Spinner />
              </div>
            ) : error ? (
              <div className="flex items-center justify-center py-16">
                <div className="text-center">
                  <AlertTriangle className="h-8 w-8 text-danger-400 mx-auto mb-2" />
                  <p className="text-sm text-slate-600">{error}</p>
                </div>
              </div>
            ) : data.length === 0 ? (
              <div className="flex items-center justify-center py-16">
                <p className="text-sm text-slate-400">No hay datos disponibles</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {data.map((item) => (
                  <button
                    key={item.id}
                    onClick={() =>
                      setSelectedEntity({
                        tipo: activeSection === 'personas' ? 'persona' : activeSection === 'cuentas' ? 'cuenta' : activeSection === 'dispositivos' ? 'dispositivo' : 'transaccion',
                        id: item.id,
                      })
                    }
                    className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-slate-50 transition-colors text-left"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-sm ${
                        activeSection === 'personas' ? 'bg-blue-50' :
                        activeSection === 'cuentas' ? 'bg-emerald-50' :
                        activeSection === 'dispositivos' ? 'bg-slate-100' :
                        'bg-danger-50'
                      }`}>
                        {activeSection === 'personas' ? '👤' :
                         activeSection === 'cuentas' ? '🏦' :
                         activeSection === 'dispositivos' ? '📱' : '⚠️'}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-slate-700 truncate">
                          {activeSection === 'personas'
                            ? `${item.nombre} ${item.apellido}`
                            : activeSection === 'cuentas'
                            ? item.alias || item.numeroCuenta
                            : activeSection === 'dispositivos'
                            ? item.fingerprint
                            : item.motivoAlerta || `TX ${item.numeroOrden}`}
                        </p>
                        <p className="text-xs text-slate-400">
                          {activeSection === 'personas'
                            ? `DNI: ${item.dni} · Riesgo: ${item.nivelRiesgo}`
                            : activeSection === 'cuentas'
                            ? `${item.banco} · ${item.tipoCuenta}`
                            : activeSection === 'dispositivos'
                            ? `${item.tipoDispositivo} · ${item.ipAddress}`
                            : `${formatCurrency(item.monto)} · ${formatDate(item.fechaTransaccion)}`}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {item.estado === 'BLOQUEADA' && (
                        <Badge variant="danger" size="sm" dot>Bloqueada</Badge>
                      )}
                      {item.esAlertada && (
                        <Badge variant="danger" size="sm" dot>Alerta</Badge>
                      )}
                      {item.esSancionado && (
                        <Badge variant="danger" size="sm">Sancionado</Badge>
                      )}
                      {item.esPEP && (
                        <Badge variant="warning" size="sm">PEP</Badge>
                      )}
                      <ChevronRight className="h-4 w-4 text-slate-300" />
                    </div>
                  </button>
                ))}
              </div>
            )}
          </Card>
        </div>

        <div>
          <SimulatorForm onSuccess={handleSimulatorSuccess} />
        </div>
      </div>

      <EntityDetailsDrawer
        entity={selectedEntity}
        onClose={() => setSelectedEntity(null)}
        onUpdate={() => fetchData(activeSection)}
      />
    </div>
  );
}
