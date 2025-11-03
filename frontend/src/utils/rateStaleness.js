// Utility to classify FX rate freshness.
// Expects an ISO date string (rateDate) and optional thresholds from fxSettings:
// fxSettings.refreshCron may imply daily, but we will use explicit stalenessWarnDays & stalenessStaleDays if later added.
// For now we derive defaults: warn after 2 days, stale after 5 days.

export function daysSince(dateStr) {
  if (!dateStr) return Infinity
  const d = new Date(dateStr + 'T00:00:00Z')
  if (Number.isNaN(d.getTime())) return Infinity
  const now = Date.now()
  const diffMs = now - d.getTime()
  return Math.floor(diffMs / (1000 * 60 * 60 * 24))
}

export function classifyStaleness(rateDate, { warnAfterDays = 2, staleAfterDays = 5 } = {}) {
  const days = daysSince(rateDate)
  if (days === Infinity) return { level: 'unknown', days }
  if (days >= staleAfterDays) return { level: 'stale', days }
  if (days >= warnAfterDays) return { level: 'warn', days }
  return { level: 'fresh', days }
}

// Returns a color token for MUI usage based on classification
export function stalenessColor(level) {
  switch (level) {
    case 'fresh': return 'success.main'
    case 'warn': return 'warning.main'
    case 'stale': return 'error.main'
    default: return 'text.disabled'
  }
}
