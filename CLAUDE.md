# CLAUDE.md — rules for AI agents working in this repo

## Project
Product catalogue proof of concept that must grow into the real product (not a throwaway).
Customers browse, search, view products and send an enquiry that emails the sales inbox.

## Layout
- `android/` — Android client. Kotlin, Jetpack Compose, MVVM. Opened on its own in Android Studio.
- `backend/` — Node.js + TypeScript API. Runs via `docker compose up --build` from the repo root.
- `notes/ai-log.md` — running log of prompts, AI output and the developer's decisions. Append after every phase.
- `DECISIONS.md`, `AI_USAGE.md`, `README.md` — at repo root.

## Hard rules
1. **The Android app never calls Fake Store (or any product provider) directly.** All product data goes through our backend API.
2. **Product data source is behind an interface** (`ProductSource` in backend). Fake Store is one adapter. Provider-specific shapes never leave the adapter — map to our own `Product` model.
3. **Email is behind an interface** (`EmailSender`). SMTP→Mailpit locally; swapping provider = new implementation, no route changes.
4. **Look and feel is swappable**: Android UI uses theme/design tokens, no hard-coded colours or sizes in screens.
5. **Every feature ships with tests.** Backend: tests use a fake `ProductSource`/`EmailSender`, never the network. Android: ViewModel tests with fake repositories; MockWebServer for the API layer.
6. **No secrets in git.** Config via env vars; document every variable in `.env.example`.
7. **Validate all input on the backend** (zod) — the client is not trusted. Escape user input in emails. Rate-limit enquiries.
8. **No paid services or accounts** needed to run locally.

## Conventions
- Android: one `UiState` sealed class (Loading / Success / Error) reused by every screen. Screens are stateless composables; ViewModels own state.
- Android emulator reaches the backend at `http://10.0.2.2:3000` (BuildConfig). Cleartext HTTP allowed in **debug only**.
- Keep it small: prefer the simplest thing that keeps the boundaries clean. No new libraries without a reason recorded in DECISIONS.md.
- Small commits, one per phase/screen, conventional-commit style (`feat(android): ...`).
