# Local AI Gateway

A self-hosted stack that lets VS Code (or any client) talk to local models running
in Ollama through one gateway that owns conversation history, permissions, and
model routing.

```
VS Code (local/Codespace)
        │
        ▼
Agent Runtime (context, tools, planning, file ops)
        │
        ▼
Local AI Gateway — Spring Boot (API/Auth, Conversation Service, Permission Engine, Model Router)
        │                                   │
        ▼                                   ▼
   PostgreSQL                            Ollama (Qwen, Llama, Mistral, ...)
(conversations, messages, projects,
 permissions, tool events)
        ▲
        │
   Dashboard (chat history, search, projects, permissions, agent activity, models)
```

This repo is a **minimal but working** version of that diagram, built so you can
run it today and grow it incrementally.

## What's included

| Piece | Tech | Status |
|---|---|---|
| Local AI Gateway | Spring Boot 3 / Java 17 | REST API, JPA + H2 (Postgres profile ready), Ollama client, model router, permission engine |
| Dashboard | Vanilla HTML/JS (no build step) | Chat, chat history, projects, permissions, agent activity, model list |
| Agent Runtime | Node.js CLI | Calls the gateway, runs a tool-call loop (read/write/list files) with permission checks |
| VS Code | — | Not built yet — for now, use the CLI agent runtime or the dashboard directly |

## Current setup: no Docker, no Postgres required

For daily use right now, the stack is deliberately minimal:

```
Dashboard (static HTML/JS) ──► Gateway (Spring Boot + H2, file-based DB) ──► Ollama API
```

H2 stores data in a local file (`backend/data/local-ai-gateway.mv.db`) — no DB
server to install or run. Postgres is still fully wired up behind a Spring
profile for when you want it (see **Moving to Postgres later** below); nothing
needs to be rewritten to switch.

## Prerequisites

- Java 17 + Maven
- [Ollama](https://ollama.com) installed natively, **or** run it via
  `docker compose up -d` (uses the minimal `docker-compose.yml`, which now
  only runs Ollama)
- Node.js 18+ if you want to use the agent runtime CLI

## Quickstart

```bash
# 1. Ollama - either:
ollama serve                          # if installed natively
#   or
docker compose up -d                  # starts just the Ollama container

ollama pull qwen2.5:7b                # (or `docker exec -it local-ai-ollama ollama pull qwen2.5:7b`)

# 2. Backend (H2 is the default profile - nothing else to configure)
cd backend
mvn spring-boot:run
# gateway listening on http://localhost:8080

# 3. Dashboard - just serve the static folder
cd ../dashboard
python3 -m http.server 8081
# open http://localhost:8081
```

That's it — three processes, no database to install. The H2 console is
available at `http://localhost:8080/h2-console` if you want to poke at the
data directly (JDBC URL: `jdbc:h2:file:./data/local-ai-gateway`).

## Moving to Postgres later

When you're ready:

```bash
docker compose -f docker-compose.full.yml up -d postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

The `postgres` Spring profile in `application.yml` has the connection details
(overridable with `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`). Same
entities, same API, same dashboard — only the datasource changes.
`docker-compose.full.yml` also has the fully containerized version (backend +
dashboard + postgres + ollama) for whenever you want everything in Docker.

## Using the agent runtime (CLI, stands in for the VS Code extension for now)

```bash
cd agent-runtime
npm install    # no dependencies yet, but keeps the workflow consistent
GATEWAY_URL=http://localhost:8080 node agent.js --workspace /path/to/your/repo "list the files in src/"

# or interactive mode
node agent.js --workspace /path/to/your/repo
> summarize what this repo does
```

The agent runtime asks for approval (`[y/N]`) before running any tool that has
no explicit `ALLOW`/`DENY` rule in the dashboard's Permissions tab — that's the
Permission Engine in action end to end.

## Daily-use workflow

1. **Start the stack** — `ollama serve` (or `docker compose up -d` for just
   Ollama), then `mvn spring-boot:run` in `backend/`, then serve `dashboard/`.
2. **Open the dashboard** at `http://localhost:8081` to chat, or use the CLI
   agent runtime from your project's terminal when you want file/tool access.
3. **Create a Project** in the dashboard for each repo you work in — set its
   default model and workspace path so you don't have to specify them every time.
4. **Set permission rules** for tools you're comfortable automating
   (e.g. `ALLOW` for `read_file`, `ASK` or `DENY` for `write_file` until you
   trust the flow).
5. **Review Agent Activity** periodically to see what tools were called, by
   which conversation, and with what result — this is your audit trail.
6. **Iterate on the backend** in `backend/src/main/java/com/localai/gateway/`
   — the layout mirrors the architecture diagram 1:1 (`controller` = API,
   `service` = Conversation Service / Permission Engine / Model Router,
   `repository`/`model` = persistence).

## Project structure

```
local-ai-gateway/
├── docker-compose.yml           # minimal: just Ollama
├── docker-compose.full.yml      # postgres + ollama + backend + dashboard, for later
├── .env.example
├── backend/                    # Local AI Gateway (Spring Boot)
│   ├── pom.xml
│   └── src/main/java/com/localai/gateway/
│       ├── GatewayApplication.java
│       ├── config/              # CORS etc.
│       ├── controller/          # REST API — Chat, Conversations, Projects,
│       │                        #   Permissions, ToolEvents, Models
│       ├── service/              # ConversationService, ModelRouterService,
│       │                        #   PermissionService, OllamaClient
│       ├── repository/           # Spring Data JPA repositories
│       ├── model/                # Project, Conversation, Message,
│       │                        #   Permission, ToolEvent entities
│       └── dto/                  # request/response payloads
├── dashboard/                   # static HTML/JS/CSS, no build step
│   ├── index.html
│   ├── app.js
│   └── styles.css
└── agent-runtime/                # Node CLI standing in for the VS Code side
    ├── package.json
    └── agent.js
```

## API surface (v0)

- `POST /api/chat` — send a message, get a reply; creates a conversation if `conversationId` is omitted
- `GET /api/conversations`, `GET /api/conversations/{id}`, `DELETE /api/conversations/{id}`
- `GET/POST/PUT/DELETE /api/projects`
- `GET/POST/PUT/DELETE /api/permissions`, `GET /api/permissions/check?toolName=&projectId=`
- `GET/POST/PUT /api/tool-events`
- `GET /api/models` — proxies Ollama's `/api/tags`

## Ideas to make this more useful (roughly in order of impact)

**Near-term, high value**
- **Streaming responses** — switch `/api/chat` to Server-Sent Events or WebSocket so the dashboard shows tokens as they arrive instead of waiting for the full reply (biggest UX win for local models, which are slower than hosted ones).
- **Auth** — even a single shared API key/JWT on the gateway, since right now anything on your network can call it.
- **Structured tool calling** — Ollama's newer models support native function calling; moving off the "reply with JSON" convention in `agent.js` would be more reliable than string parsing.
- **Conversation titles via the model** — auto-generate a better title after the first exchange instead of truncating the first message.

**Makes it genuinely more capable**
- **RAG over your codebase** — add `pgvector` to Postgres, embed files with a local embedding model, and give the agent a `search_codebase` tool instead of relying on `read_file`/`list_dir` alone.
- **More tools** — `run_command` (sandboxed!), `apply_patch`/`git_diff` so the agent proposes diffs instead of overwriting files outright, `search_web`.
- **Model fallback chains** — if the primary model errors or is unavailable, the Model Router retries with a fallback model automatically.
- **Approval queue in the dashboard** — instead of `[y/N]` in the terminal, "ASK" tool calls show up in the dashboard for you to approve/deny from anywhere, with the agent runtime polling for the decision.

**Nice quality-of-life**
- **Conversation search** — full-text search over `messages.content` (Postgres `tsvector` is enough at this scale).
- **Usage/cost dashboard** — token counts and wall-clock latency per model, useful for comparing Qwen vs. Llama vs. Mistral on your hardware.
- **Export conversation to Markdown** — one click from the dashboard.
- **Prompt template library** — saved system prompts per project (e.g. "code reviewer", "test writer") selectable from the chat header.
- **VS Code extension** — thin wrapper that reuses the same `/api/chat` contract but sends real editor context (open file, selection, workspace root) instead of CLI args; this is the natural next milestone toward the original diagram.

## Notes on this minimal version

- Default DB is **H2**, file-based, in `backend/data/`. It's a real SQL database (not in-memory-only), so your conversations persist across restarts — just not concurrent-writer-safe the way Postgres is, which is why it's a stepping stone rather than the long-term choice.
- `spring.jpa.hibernate.ddl-auto: update` auto-creates tables on first run — fine for a personal/dev tool, swap for Flyway/Liquibase migrations before this becomes multi-user or production-facing (matters more once you're on Postgres).
- The Permission Engine currently supports one rule per (tool, project) pair with `ALLOW`/`DENY`/`ASK`; `pathScope` exists on the entity but isn't enforced yet — glob-matching it against tool args is a good first contribution.
- The agent runtime's tool loop is unbounded — add a max-turns guard before pointing it at anything you don't want to babysit.
