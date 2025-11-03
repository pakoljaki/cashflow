import { useCallback } from 'react'
import { emitAnalytics } from './eventBus'

// Hook returning a stable logEvent function with common envelope fields.
export function useAnalytics(component) {
  return useCallback((type, payload = {}) => {
    emitAnalytics({
      ts: Date.now(),
      component,
      type,
      payload,
    })
  }, [component])
}

export default useAnalytics
