<!-- Paste into the GitHub PR (feature/catalogue-poc → main), without this line. -->

## Product catalogue PoC — Android (Kotlin/Compose) + Node/TypeScript backend

A browse / search / enquire product catalogue, built as a **foundation that can grow** rather than a throwaway prototype. The product data source, the look and feel, and the email provider can each be swapped behind one boundary.

### What's included
- **Backend** (`backend/`, Node + TypeScript + Express): versioned `/v1` API. Fake Store sits behind a `ProductSource` adapter with a TTL cache (serves a stale copy if the source is down), curated "top rated" products, server-side search, an image proxy (the app never contacts Fake Store), and an enquiry endpoint with zod validation, rate limiting, a honeypot, HTML escaping and a header-injection guard. Email goes through an `EmailSender` interface (SMTP → Mailpit).
- **Android app** (`android/`, Kotlin, Compose, MVVM + clean architecture, Hilt, Retrofit, Coil): Home (5 top-rated, category chips, search bar), Category (top rated + other products), Search (button/IME only, empty and no-results states), Product detail, Enquiry form (local + backend validation, retry on failure) and Thank you (shown only after a confirmed send). Theme built from design tokens.
- **Infra:** `docker compose up --build` starts the API and Mailpit. No accounts, no secrets.

### Acceptance criteria
| Criterion | Status |
|---|---|
| Home shows the 5 chosen top-rated products | ✅ |
| Category shows top-rated + additional products | ✅ |
| Search shows only matching products when the button is pressed | ✅ |
| Tapping a tile opens the product page | ✅ |
| Enquiry emails the sales inbox and shows "Thank you" | ✅ email visible in Mailpit |

### How to run
See **README.md** — `docker compose up --build`, open `android/` in Android Studio, run on an emulator (no config needed), and open the inbox at http://localhost:8025. A physical phone needs `adb reverse` plus one property; the README has an emulator-vs-phone table.

### Tests
- Backend: **66** tests (services, routes, adapter, cache, enquiry security) using fakes; no network.
- Android unit: **60** tests (MockWebServer repository tests, use cases, all ViewModels, validator).
- Android UI: **3** Compose tests, passing on a physical phone (form → Thank you, missing fields, failed send).

### Key decisions (full list in DECISIONS.md)
- "Chosen" top-rated = **business-curated** config behind an interface, not computed from ratings.
- Category "top rated" = curated per category, falling back to the 2 highest-rated (categories only have 4–6 products).
- Search matches **every word** across title, description and category.
- **Images proxied by product ID** (my change to the AI's proposal) → zero app dependency on the provider, and no open proxy.
- "Thank you" **only after the email is confirmed sent**; otherwise an error with retry.
- **Clean architecture + Hilt + no hard-coded URLs** on Android (my review changes to the AI's first pass).
- Search runs on the **button / keyboard action only**, as the criterion states (I considered auto-search and kept the criterion).
- Backend **warms its cache at start-up and de-duplicates concurrent upstream requests**, which cut the slow first load.
- Original launcher icon and branded splash (AndroidX SplashScreen).
- Phone number: digits only, max 10, assuming local numbers with no country code. Enforced in both the app and the backend.

### Dropped / simplified
Enquiries are emailed, not stored; curation is a JSON file (no admin UI); in-memory cache and rate limit; no pagination, offline mode, CI or deployment. Details and reasons are in DECISIONS.md §5, and next steps in §7.

### AI usage
Built with Claude (Opus 5.5) writing into the repo, while I directed each phase, reviewed every change, and ran every build, test and emulator check. Notable AI mistakes I caught: a KSP version incompatible with AGP 9 built-in Kotlin, `stringResource` calls in a non-composable grid builder, outdated Espresso that crashed on recent Android, and two UI-test bugs (asserting on an off-screen node, searching the merged semantics tree). All of these were caught by my builds and test runs. I also overruled the AI on architecture and image sourcing. The full account is in **AI_USAGE.md**, the agent rules in **CLAUDE.md**, and the raw log in **notes/ai-log.md**.

### Time spent
**Build: ~3h50m** (today, 10:17am → 2:05pm), including about **50m lost to environment issues**. Docker had to be installed, the Gradle download timed out, and **Android Studio was very slow on my Windows laptop** (sync, the first Hilt/KSP builds, the emulator). On a faster machine the build would have been noticeably quicker.
Breakdown: decisions ~15m · backend ~30m · Android ~1h15m · environment/tooling issues ~50m · testing and fixes ~45m · docs ~15m.

**Planning: ~30m, separately** (the evening before): analysing the brief, choosing the stack, and planning the phases.

Verified on the Android emulator and on a physical phone (Samsung, Android 17).
