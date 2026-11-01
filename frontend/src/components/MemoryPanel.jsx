import { useEffect, useState } from 'react'
import { fetchMemory, deleteMemoryFact } from '../api.js'

const CATEGORY_COLORS = {
  PREFERENCE: '#6366f1',
  GOAL: '#22c55e',
  FACT: '#3b82f6',
  PROJECT: '#f59e0b'
}

export default function MemoryPanel({ refreshSignal }) {
  const [facts, setFacts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchMemory()
      setFacts(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [refreshSignal])

  async function handleDelete(id) {
    setFacts(prev => prev.filter(f => f.id !== id))
    try {
      await deleteMemoryFact(id)
    } catch (err) {
      load()
    }
  }

  return (
    <div className="memory-panel">
      <div className="memory-header">
        <h2>What I remember</h2>
        <button onClick={load} className="refresh-btn">Refresh</button>
      </div>

      {loading && <div className="memory-empty">Loading...</div>}
      {error && <div className="memory-error">{error}</div>}
      {!loading && !error && facts.length === 0 && (
        <div className="memory-empty">No facts saved yet. Chat a bit and they'll show up here.</div>
      )}

      <div className="memory-list">
        {facts.map(fact => (
          <div key={fact.id} className="memory-item">
            <span
              className="memory-category"
              style={{ backgroundColor: CATEGORY_COLORS[fact.category] || '#999' }}
            >
              {fact.category}
            </span>
            <span className="memory-content">{fact.content}</span>
            <button className="memory-delete" onClick={() => handleDelete(fact.id)} title="Forget this">
              ×
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
