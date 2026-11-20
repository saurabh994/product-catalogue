# AI log

Running record of how AI (Claude, Opus 5.5, via the Claude app connected to this repo) was directed.
Each entry: prompt, what the AI produced, and **my call** (what I accepted, changed or rejected).

---

## Pre-build planning session — Thu 19 Nov, ~7:00–9:00pm IST (evening before)

Not part of the build, but it shaped it, so it's recorded here and counted in time spent.

- **Prompt:** shared the challenge PDF — "analyse this and tell me your thoughts and how fast can you build this".
- **AI output:** identified what's actually assessed (judgement, swappable boundaries, ownership), listed the gaps in the acceptance criteria (who "chooses" top-rated, thin categories in Fake Store, no search endpoint, email without paid services → Mailpit, Fake Store reliability → cache + own model). Proposed React + Node.
- **AI output:** estimated ~1–1.5h for AI to build alone, but recommended staged building with my review so the AI_USAGE notes are genuine and I can make a live change in the interview.
- **My call:** **switched the client to Android/Kotlin** — I'm an Android developer, I can defend and change that code live. Kept Node + TypeScript backend.
- **My call:** one repo with `android/` and `backend/` (one PR, one README, one `docker compose up`).
- **My call:** build in phases with a test + commit after each; Android screen by screen, foundation first.
- **My call:** create the Android project with **Android Studio's wizard** rather than having the AI write Gradle files from memory — AI-guessed build tool versions are a likely source of mismatch.

---

## Phase 0 — Kickoff — Fri 20 Nov, start 10:17am IST

- **Prompt:** "Starting now at 10:17AM. I've created the branch feature/catalogue-poc. Create a .gitignore for Android + Node, notes/ai-log.md, and a CLAUDE.md with the project rules: Kotlin/Compose client, Node TS backend, client never calls Fake Store directly, product source and email sender behind interfaces, tests required for every feature. Also set up docker-compose.yml with the backend and Mailpit, plus .env.example, and give me the commands to verify Docker is working."
- **AI output:**
  - `.gitignore` covering Android, Node, secrets (`.env` ignored, `.env.example` kept).
  - `CLAUDE.md` with 8 hard rules (no direct Fake Store calls, `ProductSource` + `EmailSender` interfaces, theme tokens, tests with fakes, no secrets, backend validation, no paid services) and conventions.
  - `docker-compose.yml`: `api` (built from `backend/`, not created yet) + `mailpit` (UI 8025, SMTP 1025).
  - `.env.example` documenting every config value.
- **AI decision to review:** compose uses `${VAR:-default}` defaults so the reviewer does **not** have to copy `.env` first — keeps setup to one command. `.env` is only for overrides.
- **Limitation noted:** the `api` service can't start until Phase 2 creates `backend/`, so Phase 0 verification runs Mailpit only.
- **Setup issue (10:20am):** `docker` not recognised in my terminal — Docker Desktop wasn't installed. Committed Phase 0 anyway (files don't depend on it); installing Docker in parallel with Phase 1 and running the Mailpit check after.
- **My call:** Accepted. Agree with compose defaults so a reviewer needs no `.env` copy step. Docker wasn't installed on my machine; installed it during Phase 1.

---

## Phase 1 — Plan — Fri 20 Nov, 10:31am IST

- **Prompt:** "Draft DECISIONS.md: the assumptions for each acceptance criterion's gaps, the API endpoints with request/response shapes, and the module structure for both the backend and the Android app. No code yet."
- **AI output:** `DECISIONS.md` draft — stack table, assumptions for all 5 criteria, versioned `/v1` API with shapes and error format, backend + Android module structure.
- **Key AI proposals (marked ⚑ in DECISIONS.md) for me to confirm or change:**
  1. Missing curated IDs are skipped, not an error.
  2. Category "top rated" = curated per-category list, fallback to 2 highest-rated.
  3. Search: all words must match (not whole phrase).
  4. Product image URLs still point at Fake Store's image host.
  5. Enquiry fields: name + email required, phone + message optional.
  6. "Thank you" only after the email actually sends; otherwise error + retry.
- **Other AI choices:** Express over Fastify (readability); price in cents, IDs as strings; stale-on-error cache; manual DI over Hilt; single Android module.
- **My call:** Accepted proposals 1, 2, 3, 5 and 6 as written. Changed 4: images go through our backend so the app has zero dependency on Fake Store. Kept the Android choices (manual DI, single module, Retrofit + kotlinx.serialization).
- **Review (10:38am):** accepted proposals 1, 2, 3, 5, 6. **Rejected 4** — I want product images routed through our backend too, so the app has zero dependency on Fake Store (not even its image host) and a provider/CDN change never touches the app.
- **AI follow-up on 4:** image endpoint `GET /v1/products/:id/image`; `imageUrl` returned as a relative path (backend can't know the emulator's `10.0.2.2` address); backend fetches by product ID only — never a client-supplied URL — to avoid an open proxy/SSRF; only `image/*` ≤5 MB, cached 1 day.

---

## Phase 2 — Backend — Fri 20 Nov, 10:44am IST

- **Prompt:** "Build the backend in backend/: ProductSource interface + Fake Store adapter with caching, curated top-rated config, search, image proxy, enquiry endpoint with zod validation, rate limiting and an SMTP EmailSender for Mailpit. Add a Dockerfile and wire it into docker-compose. Write tests using a fake product source. Then give me the commands to test it and walk me through the design using Android comparisons."
- **AI output:** Express 5 + TypeScript backend (~20 source files) matching DECISIONS.md §3–4; 61 tests across 6 files using fakes (no network); multi-stage Dockerfile.
- **How the AI checked its own work:**
  - Typecheck + all 61 tests passed on the first run — so it **deliberately broke the code** (removed HTML escaping; made the cache store "not found") to prove the tests catch it. Both mutations failed the right test.
  - Sent a real email through `SmtpEmailSender` to a local SMTP sink: correct To / Reply-To / Subject, HTML escaped.
  - No Docker daemon in the AI's workspace, so it replayed the Dockerfile steps by hand (clean `npm ci`, build, prod-only install, start) — server healthy. **Real `docker compose up` still to be verified on my machine.**
- **AI caught and fixed:** a missing `productId` gave zod's generic "expected string, received undefined" → changed to "Product is required".
- **Design points the AI added beyond the prompt (for me to understand and defend):**
  - "Not found" is **not cached** — otherwise requests for random IDs could grow the cache without limit.
  - Name field rejects line breaks — blocks **email header injection** via the Reply-To name.
  - Honeypot returns 201 but sends nothing, so bots learn nothing.
  - Request body limit 10 KB; malformed JSON → 400, too large → 413, unknown errors → 500 without leaking internals.
  - Provider data is **validated** with zod in the adapter — Fake Store is treated as untrusted input.
  - Curation file lives in `backend/config/` (outside `src/`) so it can be edited without a rebuild.
- **Risk flagged by AI:** the 5 featured IDs in `curation.json` (3, 5, 11, 18, 1) were chosen from the AI's memory of Fake Store's catalogue — verify against the live API.
- **Setup issue (11:16am):** my first test run failed — I ran the Docker test command from my home folder, so it mounted an empty `backend/`. Re-ran from the repo root.
- **My call:** Reviewed by behaviour and a design walkthrough rather than line by line, since TypeScript isn't my main language. Ran `docker compose up`, checked all endpoints in the browser, sent an enquiry and saw it in Mailpit, ran the tests in Docker: 61 passed. Featured products from `curation.json` look sensible. Agree with not caching "not found" and blocking line breaks in the name.
