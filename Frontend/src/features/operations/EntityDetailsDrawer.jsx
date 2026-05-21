import { useState, useEffect, useCallback } from 'react';
import { X, Trash2, ExternalLink } from 'lucide-react';
import Card from '../../components/ui/Card';
import Badge from '../../components/ui/Badge';
import Button from '../../components/ui/Button';
import Switch from '../../components/ui/Switch';
import Spinner from '../../components/ui/Spinner';
import apiClient from '../../services/apiClient';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

export default function EntityDetailsDrawer({ entity, onClose, onUpdate }) {
  const [details, setDetails] = useState(null);
  const [connections, setConnections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [blocking, setBlocking] = useState(false);
  const [deleting, setDeleting] = useState(null);

  const loadDetails = useCallback(async () => {
    if (!entity) return;
    setLoading(true);

    try {
      const { tipo, id } = entity;
      let data = null;
      let conns = [];

      if (tipo === 'persona') {
        const [personaRes, cuentasRes, dispRes] = await Promise.allSettled([
          apiClient.get(`/personas/${id}`),
          apiClient.get(`/cuentas/persona/${id}`),
          apiClient.get(`/dispositivos/persona/${id}`),
        ]);
        data = personaRes.status === 'fulfilled' ? personaRes.value.data : null;
        if (cuentasRes.status === 'fulfilled') {
          conns = conns.concat(
            cuentasRes.value.data.map((c) => ({ tipo: 'cuenta', ...c }))
          );
        }
        if (dispRes.status === 'fulfilled') {
          conns = conns.concat(
            dispRes.value.data.map((d) => ({ tipo: 'dispositivo', ...d }))
          );
        }
      } else if (tipo === 'cuenta') {
        const cuentaRes = await apiClient.get(`/cuentas/${id}`);
        data = cuentaRes.data;
      } else if (tipo === 'dispositivo') {
        const dispRes = await apiClient.get(`/dispositivos/${id}`);
        data = dispRes.data;
      }

      setDetails(data ? { tipo, ...data } : null);
      setConnections(conns);
    } catch (err) {
      console.error('Error loading details:', err);
    } finally {
      setLoading(false);
    }
  }, [entity]);

  useEffect(() => {
    loadDetails();
  }, [loadDetails]);

  const handleBlockToggle = async () => {
    if (!details || details.tipo !== 'cuenta') return;
    setBlocking(true);

    try {
      const nuevoEstado = details.estado === 'BLOQUEADA' ? 'ACTIVA' : 'BLOQUEADA';
      await apiClient.put(`/cuentas/${details.id}`, { estado: nuevoEstado });
      setDetails((prev) => ({ ...prev, estado: nuevoEstado }));
      onUpdate?.();
    } catch (err) {
      console.error('Error blocking:', err);
    } finally {
      setBlocking(false);
    }
  };

  const handleDelete = async (connId, connTipo) => {
    if (!confirm('¿Confirmás que querés desvincular este elemento?')) return;
    setDeleting(connId);

    try {
      if (connTipo === 'dispositivo') {
        await apiClient.delete(`/dispositivos/${connId}`);
      }
      setConnections((prev) => prev.filter((c) => c.id !== connId));
      onUpdate?.();
    } catch (err) {
      console.error('Error deleting:', err);
    } finally {
      setDeleting(null);
    }
  };

  if (!entity) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-black/20 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-96 bg-white h-full shadow-2xl overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-slate-200 px-5 py-4 flex items-center justify-between z-10">
          <h2 className="text-base font-semibold text-slate-800">Detalle de Entidad</h2>
          <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-slate-100 transition-colors">
            <X className="h-5 w-5 text-slate-500" />
          </button>
        </div>

        <div className="p-5 space-y-5">
          {loading ? (
            <Spinner className="py-12" />
          ) : details ? (
            <>
              <Card>
                <div className="flex items-center gap-3 mb-4">
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                    details.tipo === 'persona' ? 'bg-blue-100' : details.tipo === 'cuenta' ? 'bg-emerald-100' : 'bg-slate-100'
                  }`}>
                    <span className="text-lg">
                      {details.tipo === 'persona' ? '👤' : details.tipo === 'cuenta' ? '🏦' : '📱'}
                    </span>
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-800">
                      {details.tipo === 'persona'
                        ? `${details.nombre} ${details.apellido}`
                        : details.tipo === 'cuenta'
                        ? details.alias || details.numeroCuenta
                        : details.fingerprint}
                    </p>
                    <Badge
                      variant={
                        details.estado === 'BLOQUEADA' ? 'danger' : details.esSospechoso ? 'warning' : 'success'
                      }
                      size="sm"
                      dot
                    >
                      {details.estado === 'BLOQUEADA' ? 'Bloqueada' : details.esSospechoso ? 'Sospechoso' : 'Activo'}
                    </Badge>
                  </div>
                </div>

                <div className="space-y-2.5 text-xs">
                  {details.tipo === 'persona' && (
                    <>
                      <DetailRow label="DNI" value={details.dni} mono />
                      <DetailRow label="Email" value={details.email} />
                      <DetailRow label="Riesgo" value={`${details.nivelRiesgo}/100`} />
                      {details.esPEP && <DetailRow label="PEP" value="Sí" badge="warning" />}
                      {details.esSancionado && <DetailRow label="Sancionado" value="Sí" badge="danger" />}
                    </>
                  )}
                  {details.tipo === 'cuenta' && (
                    <>
                      <DetailRow label="Banco" value={details.banco} />
                      <DetailRow label="Cuenta" value={details.numeroCuenta} mono />
                      <DetailRow label="CBU/CVU" value={details.cbvu} mono />
                      <DetailRow label="Saldo" value={formatCurrency(details.saldo)} />
                      <DetailRow label="Tipo" value={details.tipoCuenta} />
                      <div className="flex items-center justify-between py-1">
                        <span className="text-slate-500">Bloqueo preventivo</span>
                        <Switch
                          checked={details.estado === 'BLOQUEADA'}
                          onChange={handleBlockToggle}
                          disabled={blocking}
                        />
                      </div>
                    </>
                  )}
                  {details.tipo === 'dispositivo' && (
                    <>
                      <DetailRow label="Tipo" value={details.tipoDispositivo} />
                      <DetailRow label="Marca" value={details.marca} />
                      <DetailRow label="Modelo" value={details.modelo} />
                      <DetailRow label="IP" value={details.ipAddress} mono />
                      <DetailRow label="País" value={details.ipPais} />
                      {details.ipEsTor && <DetailRow label="Tor" value="Sí" badge="danger" />}
                      {details.ipEsVPN && <DetailRow label="VPN" value="Sí" badge="warning" />}
                      {details.esEmulador && <DetailRow label="Emulador" value="Sí" badge="danger" />}
                      <DetailRow label="Personas" value={details.cantidadPersonasAsociadas} />
                    </>
                  )}
                </div>
              </Card>

              {connections.length > 0 && (
                <Card>
                  <h3 className="text-sm font-semibold text-slate-800 mb-3">Conexiones</h3>
                  <div className="space-y-2">
                    {connections.map((conn) => (
                      <div
                        key={conn.id}
                        className="flex items-center justify-between p-2.5 rounded-lg bg-slate-50 border border-slate-100"
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <span className="text-sm">
                            {conn.tipo === 'cuenta' ? '🏦' : '📱'}
                          </span>
                          <div className="min-w-0">
                            <p className="text-xs font-medium text-slate-700 truncate">
                              {conn.tipo === 'cuenta'
                                ? conn.alias || conn.numeroCuenta
                                : conn.fingerprint}
                            </p>
                            <p className="text-xs text-slate-400">{conn.tipo}</p>
                          </div>
                        </div>
                        <button
                          onClick={() => handleDelete(conn.id, conn.tipo)}
                          disabled={deleting === conn.id}
                          className="p-1.5 rounded-lg hover:bg-danger-50 text-slate-400 hover:text-danger-600 transition-colors disabled:opacity-50"
                          title="Desvincular"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                </Card>
              )}
            </>
          ) : (
            <div className="text-center py-12">
              <p className="text-sm text-slate-500">No se pudieron cargar los detalles</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function DetailRow({ label, value, mono = false, badge = null }) {
  return (
    <div className="flex items-center justify-between py-1">
      <span className="text-slate-500">{label}</span>
      {badge ? (
        <Badge variant={badge} size="sm">{value}</Badge>
      ) : (
        <span className={`text-slate-800 ${mono ? 'font-mono text-xs' : ''}`}>{value}</span>
      )}
    </div>
  );
}
