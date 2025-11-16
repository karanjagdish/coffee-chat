import { useEffect, useState } from 'react'
import { getApiKey, regenerateApiKey } from '../../services/user'

export function ApiKeyManager() {
  const [apiKey, setApiKey] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    let mounted = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const response = await getApiKey()
        if (response.success && response.data && mounted) {
          setApiKey(response.data.apiKey)
        } else if (response.error && mounted) {
          setError(response.error.message)
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to load API key')
        }
      } finally {
        if (mounted) {
          setLoading(false)
        }
      }
    }
    void load()
    return () => {
      mounted = false
    }
  }, [])

  async function handleRegenerate() {
    setLoading(true)
    setError(null)
    setCopied(false)
    try {
      const response = await regenerateApiKey()
      if (response.success && response.data) {
        setApiKey(response.data.apiKey)
      } else if (response.error) {
        setError(response.error.message)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to regenerate API key')
    } finally {
      setLoading(false)
    }
  }

  async function handleCopy() {
    if (!apiKey || !navigator.clipboard) return
    try {
      await navigator.clipboard.writeText(apiKey)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // ignore
    }
  }

  return (
    <section className="space-y-3 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
      <h2 className="text-sm font-semibold text-slate-100">API Key</h2>
      {error && <p className="text-xs text-red-400">{error}</p>}
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <input
            type="text"
            readOnly
            value={apiKey ?? (loading ? 'Loading…' : 'No API key')}
            className="flex-1 truncate rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-100"
          />
          <button
            type="button"
            onClick={handleCopy}
            disabled={!apiKey}
            className="rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>
        <button
          type="button"
          onClick={handleRegenerate}
          disabled={loading}
          className="rounded-md bg-emerald-500 px-3 py-1.5 text-xs font-medium text-slate-950 hover:bg-emerald-400 disabled:opacity-60"
        >
          {loading ? 'Regenerating…' : 'Regenerate API Key'}
        </button>
      </div>
    </section>
  )
}
