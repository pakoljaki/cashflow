export const CURRENCIES = ["HUF", "EUR", "USD"];

export const CURRENCY_META = {
  HUF: { symbol: "Ft", fractionDigits: 0 },
  EUR: { symbol: "€", fractionDigits: 2 },
  USD: { symbol: "$", fractionDigits: 2 }
};

export function isSupportedCurrency(code) {
  return CURRENCIES.includes(code);
}

export function getCurrencySymbol(code) {
  return CURRENCY_META[code]?.symbol || code;
}

export function getFractionDigits(code) {
  return CURRENCY_META[code]?.fractionDigits ?? 2;
}
