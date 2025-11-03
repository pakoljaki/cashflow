// Helper to build query strings from an object, ignoring null/undefined
export function buildQuery(params) {
  const search = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  return search ? `?${search}` : '';
}
