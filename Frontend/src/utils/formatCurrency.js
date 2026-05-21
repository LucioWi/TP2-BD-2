export function formatCurrency(amount, currency = 'ARS') {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) return '$0 ARS';

  const formatted = num.toLocaleString('es-AR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });

  return `$${formatted} ${currency}`;
}
