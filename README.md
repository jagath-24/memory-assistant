# AI Assistant with Long-Term Memory

A chat assistant that remembers durable facts about the user across sessions and
injects them back into future conversations — no vector DB, just a structured
MySQL fact store. Built with Spring Boot, React, MySQL, and the Groq API.

## Why structured facts instead of embeddings

v1 deliberately skips a vector store. Chat history + semantic search is the
"obvious" way to build memory, but it's overkill for what this needs to do:
remember a bounded set of durable facts (preferences, goals, ongoing projects)
and inject them cheaply into every prompt. A structured table with
category + content + dedupe-on-conflict is simpler to reason about, cheaper to
run, and easier to show the user in a memory viewer UI. Embeddings/vector
search is listed below as a v2 upgrade — the point of v1 is to prove the
extraction → storage → injection loop works end to end first.

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│  React UI    │────▶│  Spring Boot API │────▶│    MySQL    │
│ (chat +      │◀────│                  │◀────│ (facts,     │
│  memory      │     │                  │     │  sessions,  │
│  panel)      │     │                  │     │  messages)  │
└─────────────┘     └────────┬─────────┘     └─────────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │     Groq API     │
                     │  (chat + fact    │
                     │   extraction)    │
                     └──────────────────┘
```

## Extraction pipeline (the core mechanic)

Every `/api/chat` call does two Groq round-trips:

1. **Chat completion** — the user's message, recent conversation history, and
   a memory context block (built from stored facts) go to the main chat model.
   The reply comes back and is shown to the user immediately.
2. **Fact extraction** (fires after step 1, doesn't block the reply) — the
   same exchange (`user message` + `assistant reply`) is sent to a second,
   cheaper Groq call with a strict system prompt: return only a JSON array of
   `{category, content}` objects, or `[]` if nothing durable came up.
   `category` is one of `PREFERENCE | GOAL | FACT | PROJECT`.

Extracted facts are **upserted**, not blindly inserted: `MemoryService` checks
for an existing fact with the same category + content (case-insensitive exact
match) and just bumps `updatedAt` on a hit, instead of creating a duplicate
row. This is the "dedupe/update on conflict" logic from the spec — kept
simple on purpose (exact match, not fuzzy/semantic) since that's the kind of
scope-control decision worth calling out.

On the next message, `MemoryService.buildContextBlock()` pulls the most
recently updated facts (capped at 40) and prepends them to the system prompt,
so the chat model has that context without re-reading the whole conversation
history.

## Project structure

```
memory-assistant/
├── backend/                          Spring Boot app
│   ├── src/main/java/com/memoryassistant/
│   │   ├── entity/                   MemoryFact, ConversationSession, Message
│   │   ├── repository/               Spring Data JPA repos
│   │   ├── service/                  GroqService, MemoryService, ChatService
│   │   ├── controller/               ChatController, MemoryController, ConversationController
│   │   └── config/                   CORS
│   └── src/main/resources/application.yml
├── frontend/                         React (Vite) app
│   └── src/
│       ├── components/ChatPanel.jsx  Chat window
│       ├── components/MemoryPanel.jsx  Live memory viewer (view/delete facts)
│       └── api.js
├── docker-compose.yml
└── .env.example
```

## API

| Method | Endpoint                 | Description                                  |
|--------|---------------------------|-----------------------------------------------|
| POST   | `/api/chat`               | Send a message, get a reply + any new facts   |
| GET    | `/api/memory`              | List all stored facts                         |
| DELETE | `/api/memory/{factId}`     | Delete a fact                                 |
| GET    | `/api/conversations`       | List conversation sessions                    |
| POST   | `/api/conversations`       | Start a new session                           |

`POST /api/chat` body: `{ "message": "...", "sessionId": 1 }` (`sessionId` is
optional — omit it and a new session is created automatically, returned in
the response).

## Running it

### Docker (recommended)

```bash
cp .env.example .env
# put your GROQ_API_KEY in .env
docker-compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080

### Manual

**Backend** (needs Java 17, Maven, a running MySQL on 3306):
```bash
cd backend
export GROQ_API_KEY=your_key_here
mvn spring-boot:run
```

**Frontend**:
```bash
cd frontend
npm install
npm run dev
```

Get a free Groq API key at https://console.groq.com.

## v2 roadmap (not built here — scope control)

- Swap exact-match dedupe for embedding-similarity dedupe (pgvector or a
  lightweight vector store) so near-duplicate facts ("likes Java" vs "prefers
  Java") collapse into one.
- Per-user auth (JWT) — current version assumes a single user, no `user_id`
  column anywhere.
- Fact confidence scores + decay (stale facts should rank lower in context).
- Streaming responses (SSE) instead of waiting for the full Groq reply.
