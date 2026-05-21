export function formatDate(dateString) {
  if (!dateString) return 'Sin fecha';

  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return 'Hace unos segundos';
  if (diffMin < 60) return `Hace ${diffMin} minuto${diffMin > 1 ? 's' : ''}`;
  if (diffHour < 24) return `Hace ${diffHour} hora${diffHour > 1 ? 's' : ''}`;
  if (diffDay < 7) return `Hace ${diffDay} día${diffDay > 1 ? 's' : ''}`;
  if (diffDay < 30) return `Hace ${Math.floor(diffDay / 7)} semana${Math.floor(diffDay / 7) > 1 ? 's' : ''}`;

  return date.toLocaleDateString('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

export function formatDateTime(dateString) {
  if (!dateString) return 'Sin fecha';

  const date = new Date(dateString);
  return date.toLocaleDateString('es-AR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
