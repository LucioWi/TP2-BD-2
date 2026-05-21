import { useState, useCallback } from 'react';
import { Search, X } from 'lucide-react';
import apiClient from '../services/apiClient';
import Spinner from './ui/Spinner';

export default function Topbar({ onSearchResult }) {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSearch = useCallback(async () => {
    if (!query.trim()) return;

    setLoading(true);
    setError(null);

    try {
      const results = { personas: [], cuentas: [], dispositivos: [] };

      if (/^\d+$/.test(query.trim())) {
        const [personaRes, cuentaRes] = await Promise.allSettled([
          apiClient.get(`/personas/dni/${query.trim()}`),
          apiClient.get(`/cuentas/numero/${query.trim()}`),
        ]);
        if (personaRes.status === 'fulfilled') results.personas.push(personaRes.value.data);
        if (cuentaRes.status === 'fulfilled') results.cuentas.push(cuentaRes.value.data);
      } else if (/^fp-/.test(query.trim())) {
        const [dispRes] = await Promise.allSettled([
          apiClient.get('/dispositivos'),
        ]);
        if (dispRes.status === 'fulfilled') {
          results.dispositivos = dispRes.value.data.filter((d) =>
            d.fingerprint?.toLowerCase().includes(query.trim().toLowerCase())
          );
        }
      } else if (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(query.trim())) {
        const [dispRes] = await Promise.allSettled([
          apiClient.get('/dispositivos'),
        ]);
        if (dispRes.status === 'fulfilled') {
          results.dispositivos = dispRes.value.data.filter((d) => d.ipAddress === query.trim());
        }
      } else {
        const [personasRes] = await Promise.allSettled([
          apiClient.get('/personas'),
        ]);
        if (personasRes.status === 'fulfilled') {
          results.personas = personasRes.value.data.filter(
            (p) =>
              p.nombre?.toLowerCase().includes(query.trim().toLowerCase()) ||
              p.apellido?.toLowerCase().includes(query.trim().toLowerCase()) ||
              p.email?.toLowerCase().includes(query.trim().toLowerCase())
          );
        }
      }

      onSearchResult?.(results);
    } catch (err) {
      setError('Error al buscar. Intentá nuevamente.');
    } finally {
      setLoading(false);
    }
  }, [query, onSearchResult]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleSearch();
  };

  return (
    <header className="sticky top-0 z-30 bg-white/80 backdrop-blur-sm border-b border-slate-200 px-6 py-3">
      <div className="flex items-center gap-4 max-w-4xl mx-auto">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Buscar por nombre, DNI, IP, cuenta o fingerprint..."
            className="w-full pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 transition-all"
          />
          {query && (
            <button
              onClick={() => {
                setQuery('');
                onSearchResult?.(null);
              }}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
        {loading && <Spinner size="sm" />}
        {error && <span className="text-xs text-danger-600">{error}</span>}
      </div>
    </header>
  );
}
