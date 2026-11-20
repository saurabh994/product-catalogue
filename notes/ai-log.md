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
- **My call:** _(fill in)_
