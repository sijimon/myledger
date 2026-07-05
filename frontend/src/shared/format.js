// Neutral fallback prefix when no currency code is supplied (e.g. cross-currency views).
const CURRENCY_PREFIX = '';

// Common currency codes offered when creating/editing a project.
export const CURRENCIES = ['USD', 'INR', 'EUR', 'GBP', 'AED', 'AUD', 'CAD', 'SGD', 'JPY'];

const number = new Intl.NumberFormat(undefined, {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function money(value, currency) {
  const n = typeof value === 'number' ? value : Number(value ?? 0);
  const safe = Number.isFinite(n) ? n : 0;
  if (currency) {
    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency', currency,
        minimumFractionDigits: 2, maximumFractionDigits: 2,
      }).format(safe);
    } catch {
      return `${currency} ${number.format(safe)}`;
    }
  }
  return CURRENCY_PREFIX + number.format(safe);
}

