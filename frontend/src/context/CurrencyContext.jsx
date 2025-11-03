import React, { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react'
import PropTypes from 'prop-types'
import { CURRENCIES, isSupportedCurrency } from '../constants/currencies'

/*
 CurrencyContext responsibilities:
  - displayCurrency: what user wants to see amounts in
  - basePlanCurrency: currency of the currently selected plan (set by pages that load a plan)
  - quotes: list of available currencies (static for now)
  - setDisplayCurrency, setBasePlanCurrency
  - format helpers placeholder (will integrate number formatter later)
*/

const DEFAULT_DISPLAY = 'HUF';
const LS_KEY = 'displayCurrency';

export const CurrencyContext = createContext({
  displayCurrency: DEFAULT_DISPLAY,
  basePlanCurrency: DEFAULT_DISPLAY,
  quotes: CURRENCIES,
  setDisplayCurrency: () => {},
  setBasePlanCurrency: () => {},
  fxSettings: null,
  fxEnabled: true,
  refreshFxSettings: () => {},
  roles: [],
});

export const CurrencyProvider = ({ children }) => {
  // Provide a universal root object reference (browser window preferred)
  const root = useMemo(() => {
    /* eslint-disable no-undef */
    if (typeof globalThis !== 'undefined') {
      return globalThis
    }
    /* eslint-enable no-undef */
    return {}
  }, [])

  const initDisplayCurrency = () => {
    let stored
    try {
        stored = root.localStorage?.getItem(LS_KEY)
    } catch(e) {
      console.warn('FX: unable to read displayCurrency from storage', e)
    }
    return stored && isSupportedCurrency(stored) ? stored : DEFAULT_DISPLAY
  }
  // Initialize display currency from localStorage; eslint rule falsely flags destructuring so suppressed.
  // eslint-disable-next-line
  const [displayCurrency, setDisplayCurrencyState] = useState(initDisplayCurrency())
  const [basePlanCurrency, setBasePlanCurrency] = useState(DEFAULT_DISPLAY)
  const [fxSettings, setFxSettings] = useState(null)
  const [fxEnabled, setFxEnabled] = useState(true)
  const [roles, setRoles] = useState([])
  // Simple in-memory cache for KPI responses keyed by year|base|display
  const [kpiCache, setKpiCache] = useState({})
  // Feature flags: allow runtime toggling of subsets (can be extended via settings or env)
  const [fxFeatureFlags, setFxFeatureFlags] = useState({
    dualAmounts: true,
    stalenessBadges: true,
    rateBanner: true,
    healthPanel: true,
    optimisticSwitch: true,
  })

  const setDisplayCurrency = useCallback((code) => {
    if (!isSupportedCurrency(code)) return;
    setDisplayCurrencyState(code);
    try {
        root.localStorage?.setItem(LS_KEY, code)
    } catch(e) {
      console.warn('FX: unable to persist displayCurrency', e)
    }
  // root only contains stable globalThis reference; safe to exclude
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Sync if localStorage is changed in another tab
  useEffect(() => {
    const handler = (e) => {
      if (e.key === LS_KEY && e.newValue && isSupportedCurrency(e.newValue)) {
        setDisplayCurrencyState(e.newValue);
      }
    };
      root.addEventListener?.('storage', handler)
      return () => root.removeEventListener?.('storage', handler)
  // root is stable global; ignoring dependency
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Helper: safely extract roles/authorities from JWT token
  const extractRolesFromToken = useCallback((jwt) => {
    if (!jwt) return []
    const segments = jwt.split('.')
    if (segments.length !== 3) return []
    try {
      const decoded = JSON.parse(root.atob(segments[1]))
      const raw = decoded.roles || decoded.authorities || decoded.auth || []
      if (Array.isArray(raw)) return raw
      if (typeof raw === 'string') return raw.split(/[,\s]+/).filter(Boolean)
      return []
    } catch(e) {
      console.warn('FX: unable to decode JWT roles', e)
      return []
    }
  }, [root])

  // Initial roles extraction
  useEffect(() => {
    const token = localStorage.getItem('token')
    setRoles(extractRolesFromToken(token))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const refreshFxSettings = useCallback(async () => {
    const token = localStorage.getItem('token')
    if (!token) return
    try {
      const res = await fetch('/api/settings/fx', { headers: { Authorization: `Bearer ${token}` } })
      if (!res.ok) return
      const data = await res.json()
      setFxSettings(data)
      if (typeof data.enabled === 'boolean') setFxEnabled(data.enabled)
  } catch(e) { console.warn('FX: unable to fetch settings', e) }
  }, [])

  useEffect(() => { refreshFxSettings() }, [refreshFxSettings])

  const kpiCacheKey = useCallback((year, base, display) => `${year}|${base}|${display}`, [])
  const getCachedKpi = useCallback((year, base, display) => {
    const key = kpiCacheKey(year, base, display)
    return kpiCache[key]
  }, [kpiCache, kpiCacheKey])
  const setCachedKpi = useCallback((year, base, display, data) => {
    const key = kpiCacheKey(year, base, display)
    setKpiCache(prev => ({ ...prev, [key]: { data, ts: Date.now() } }))
  }, [kpiCacheKey])

  const unsupported = !isSupportedCurrency(displayCurrency)
  const effectiveDisplayCurrency = unsupported ? DEFAULT_DISPLAY : displayCurrency

  // Effective flags honor global fxEnabled switch for graceful degradation
  const fxFlags = fxEnabled ? fxFeatureFlags : {
    dualAmounts: false,
    stalenessBadges: false,
    rateBanner: false,
    healthPanel: false,
    optimisticSwitch: false,
  }

  const value = useMemo(() => ({
    displayCurrency, // raw user selection
    effectiveDisplayCurrency,
    basePlanCurrency,
    quotes: CURRENCIES,
    setDisplayCurrency,
    setBasePlanCurrency,
    fxSettings,
    fxEnabled,
    refreshFxSettings,
    roles,
    hasRole: (r) => roles.includes(r),
    warnAfterDays: fxSettings?.warnAfterDays ?? 2,
    staleAfterDays: fxSettings?.staleAfterDays ?? 5,
    unsupportedDisplayCurrency: unsupported,
    getCachedKpi,
    setCachedKpi,
    fxFeatureFlags, // raw adjustable flags
    setFxFeatureFlags,
    fxFlags,        // effective flags (considering fxEnabled)
    aggregateRateStaleness: (monthlyData = [], { warnAfterDays: warn = fxSettings?.warnAfterDays ?? 2, staleAfterDays: stale = fxSettings?.staleAfterDays ?? 5 } = {}) => {
      // Find oldest rateDate among entries where a rateDate exists.
      let oldestDate = null
      for (const row of monthlyData) {
        const rd = row?.rateDate
        if (!rd) continue
        if (!oldestDate || rd < oldestDate) oldestDate = rd
      }
      if (!oldestDate) return null
      // Reuse classifyStaleness logic dynamically (import lazily to avoid circular refs if any)
      try {
        // dynamic import is not necessary, but to keep context decoupled we replicate minimal logic
        const d = new Date(oldestDate + 'T00:00:00Z')
        const diffDays = Math.floor((Date.now() - d.getTime()) / (1000 * 60 * 60 * 24))
        let level = 'fresh'
        if (diffDays >= stale) level = 'stale'
        else if (diffDays >= warn) level = 'warn'
        return { level, maxDays: diffDays, oldestDate }
      } catch { return { level: 'unknown', maxDays: Infinity, oldestDate } }
    }
  }), [displayCurrency, effectiveDisplayCurrency, basePlanCurrency, setDisplayCurrency, fxSettings, fxEnabled, refreshFxSettings, roles, unsupported, fxFeatureFlags, fxFlags, getCachedKpi, setCachedKpi])

  return (
    <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>
  )
};

export function useCurrency() {
  return useContext(CurrencyContext)
}

CurrencyProvider.propTypes = {
  children: PropTypes.node,
}
