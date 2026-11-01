import { useEffect, useRef, useState } from 'react'
import { sendChatMessage, fetchMessages } from '../api.js'

export default function ChatPanel({ sessionId, onSessionCreated, onNewFacts, onTitleUpdate }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => {
    if (sessionId) {
      setMessages([])
      fetchMessages(sessionId)
        .then(setMessages)
        .catch(() => setMessages([]))
    } else {
      setMessages([])
    }
  }, [sessionId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  async function handleSend(e) {
    e.preventDefault()
    const text = input.trim()
    if (!text || loading) return

    setMessages(prev => [...prev, { role: 'user', content: text }])
    setInput('')
    setLoading(true)

    try {
      const res = await sendChatMessage(text, sessionId)
      setMessages(prev => [...prev, { role: 'assistant', content: res.reply }])
      if (!sessionId && res.sessionId) onSessionCreated(res.sessionId)
      if (res.sessionTitle) onTitleUpdate(res.sessionId, res.sessionTitle)
      // Facts are extracted async in the background — trigger a refresh after a short delay
      setTimeout(() => onNewFacts(), 3000)
    } catch (err) {
      setMessages(prev => [...prev, { role: 'assistant', content: `Error: ${err.message}` }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat-panel">
      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="chat-empty">Say something to get started.</div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`chat-bubble ${m.role}`}>
            {m.content}
          </div>
        ))}
        {loading && <div className="chat-bubble assistant loading">Thinking...</div>}
        <div ref={bottomRef} />
      </div>
      <form className="chat-input-row" onSubmit={handleSend}>
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="Type a message..."
          disabled={loading}
        />
        <button type="submit" disabled={loading || !input.trim()}>Send</button>
      </form>
    </div>
  )
}
