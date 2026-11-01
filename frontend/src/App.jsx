import { useEffect, useState } from 'react'
import ChatPanel from './components/ChatPanel.jsx'
import MemoryPanel from './components/MemoryPanel.jsx'
import { fetchConversations, createConversation, retitleConversations } from './api.js'

export default function App() {
  const [sessionId, setSessionId] = useState(null)
  const [conversations, setConversations] = useState([])
  const [memoryRefreshSignal, setMemoryRefreshSignal] = useState(0)

  useEffect(() => {
    // On load: back-fill any "New conversation" titles, then show the updated list
    retitleConversations().then(setConversations).catch(() => {
      fetchConversations().then(setConversations).catch(() => {})
    })
  }, [])

  function handleSessionCreated(newId) {
    setSessionId(newId)
    fetchConversations().then(setConversations).catch(() => {})
  }

  function handleTitleUpdate(id, title) {
    setConversations(prev =>
      prev.map(c => c.id === id ? { ...c, title } : c)
    )
  }

  async function handleNewChat() {
    const convo = await createConversation()
    setSessionId(convo.id)
    setConversations(prev => [convo, ...prev])
  }

  function handleNewFacts() {
    setMemoryRefreshSignal(s => s + 1)
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <button className="new-chat-btn" onClick={handleNewChat}>+ New chat</button>
        <div className="convo-list">
          {conversations.map(c => (
            <button
              key={c.id}
              className={`convo-item ${c.id === sessionId ? 'active' : ''}`}
              onClick={() => setSessionId(c.id)}
            >
              {c.title}
            </button>
          ))}
        </div>
      </aside>

      <main className="main">
        <ChatPanel
          sessionId={sessionId}
          onSessionCreated={handleSessionCreated}
          onNewFacts={handleNewFacts}
          onTitleUpdate={handleTitleUpdate}
        />
      </main>

      <aside className="memory-sidebar">
        <MemoryPanel refreshSignal={memoryRefreshSignal} />
      </aside>
    </div>
  )
}
