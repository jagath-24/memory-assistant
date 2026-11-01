const BASE = '/api'

async function handle(res) {
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Request failed (${res.status}): ${text}`)
  }
  return res.json()
}

export async function sendChatMessage(message, sessionId) {
  const res = await fetch(`${BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, sessionId })
  })
  return handle(res)
}

export async function fetchMemory() {
  const res = await fetch(`${BASE}/memory`)
  return handle(res)
}

export async function deleteMemoryFact(factId) {
  const res = await fetch(`${BASE}/memory/${factId}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`Failed to delete fact (${res.status})`)
}

export async function fetchMessages(sessionId) {
  const res = await fetch(`${BASE}/conversations/${sessionId}/messages`)
  return handle(res)
}

export async function fetchConversations() {
  const res = await fetch(`${BASE}/conversations`)
  return handle(res)
}

export async function retitleConversations() {
  const res = await fetch(`${BASE}/conversations/retitle`, { method: 'POST' })
  return handle(res)
}

export async function createConversation(title) {
  const res = await fetch(`${BASE}/conversations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title })
  })
  return handle(res)
}
