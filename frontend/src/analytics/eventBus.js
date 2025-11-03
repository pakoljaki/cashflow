// Simple in-memory analytics event bus. Can later be swapped for a network dispatcher.
const listeners = new Set()

export function onAnalytics(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function emitAnalytics(event) {
  for (const l of listeners) {
    try { l(event) } catch (e) { /* swallow */ }
  }
  if (process.env.NODE_ENV !== 'production') {
    // eslint-disable-next-line no-console
    console.debug('[analytics]', event)
  }
}
