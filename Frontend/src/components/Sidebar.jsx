import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Network, ClipboardList, Shield } from 'lucide-react';

const navItems = [
  {
    to: '/',
    icon: LayoutDashboard,
    label: 'Dashboard',
    description: 'Vista ejecutiva',
  },
  {
    to: '/investigador',
    icon: Network,
    label: 'Investigador',
    description: 'Visor de grafos',
  },
  {
    to: '/operaciones',
    icon: ClipboardList,
    label: 'Operaciones',
    description: 'Gestión CRUD',
  },
];

export default function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 h-screen w-64 bg-white border-r border-slate-200 flex flex-col z-40">
      <div className="p-5 border-b border-slate-100">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center">
            <Shield className="h-5 w-5 text-white" />
          </div>
          <div>
            <h1 className="text-base font-semibold text-slate-800">SOC</h1>
            <p className="text-xs text-slate-500">Ciberseguridad</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 p-3 space-y-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-150 group ${
                isActive
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-800'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <item.icon
                  className={`h-5 w-5 ${
                    isActive ? 'text-emerald-600' : 'text-slate-400 group-hover:text-slate-600'
                  }`}
                />
                <div>
                  <span className="text-sm font-medium block">{item.label}</span>
                  <span className="text-xs opacity-70">{item.description}</span>
                </div>
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-slate-100">
        <div className="text-xs text-slate-400 text-center">
          Sistema de Detección de Fraude v1.0
        </div>
      </div>
    </aside>
  );
}
